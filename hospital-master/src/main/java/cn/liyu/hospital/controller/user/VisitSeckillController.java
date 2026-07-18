package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.UserHolder;
import cn.liyu.hospital.component.RedisIdWorker;
import cn.liyu.hospital.dto.SeckillStockDTO;
import cn.liyu.hospital.dto.TimePeriodEnum;
import cn.liyu.hospital.dto.UserDTO;
import cn.liyu.hospital.dto.VisitPlanDTO;
import cn.liyu.hospital.service.IUserMedicalCardService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import cn.liyu.hospital.service.IVisitPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 专家号秒杀抢号控制器
 * <p>
 * 核心流程：Lua脚本原子校验（含XADD Stream消息） → Controller快速返回orderId → Stream消费者异步写入MySQL
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Tag(name = "用户端 · 秒杀抢号", description = "专家号限量秒杀抢号")
@RestController
@RequestMapping("/visit")
public class VisitSeckillController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisitSeckillController.class);

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String ORDER_KEY_PREFIX = "seckill:order:";
    private static final String LOCK_KEY_PREFIX = "lock:seckill:order:";

    /**
     * 秒杀 Lua 脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Resource
    private IUserMedicalCardService userMedicalCardService;

    @Resource
    private IVisitPlanService visitPlanService;

    /**
     * 查询秒杀号源剩余库存
     */
    @Operation(summary = "查询秒杀号源库存", description = "传入出诊计划编号")
    @GetMapping("/seckill/{planId}/stock")
    public CommonResult<SeckillStockDTO> getStock(@PathVariable Long planId) {
        String stockKey = STOCK_KEY_PREFIX + planId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);

        SeckillStockDTO dto = new SeckillStockDTO();
        dto.setPlanId(planId);

        if (stockStr == null) {
            dto.setStock(0L);
            dto.setSoldOut(true);
        } else {
            long stock = Long.parseLong(stockStr);
            dto.setStock(stock);
            dto.setSoldOut(stock <= 0);
        }

        return CommonResult.success(dto);
    }

    /**
     * 秒杀抢号（Lua 原子操作 + Stream 异步下单）
     * <p>
     * 账号从 JWT 登录态获取（UserHolder），就诊卡由账号反查（一人一卡），
     * 均不信任前端传值，防止伪造他人身份抢号。
     */
    @Operation(summary = "秒杀抢号", description = "传入出诊计划编号、时间段（需登录，账号与就诊卡由登录态推导）")
    @PostMapping("/seckill/{planId}")
    public CommonResult<java.util.Map<String, Object>> seckill(@PathVariable Long planId,
                                 @RequestParam Integer timePeriod) {
        // 0. 登录态校验：账号取自 JWT（过滤器已存入 ThreadLocal）
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return CommonResult.validateFailed("请先登录！");
        }
        Long accountId = user.getId();

        // 0.1 一人一卡：由账号反查唯一就诊卡
        Optional<Long> cardIdOptional = userMedicalCardService.getCardIdByAccountId(accountId);
        if (!cardIdOptional.isPresent()) {
            return CommonResult.validateFailed("未找到就诊卡，请先完善就诊卡信息！");
        }
        Long cardId = cardIdOptional.get();

        // 1. Redisson 分布式锁（防重复提交）
        String lockKey = LOCK_KEY_PREFIX + planId + ":" + cardId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                return CommonResult.failed("系统繁忙，请稍后再试！");
            }

            // 2. 校验出诊计划存在，并按计划的上午/下午修正 timePeriod（服务端推算，不信任前端传值）
            Optional<VisitPlanDTO> planOptional = visitPlanService.getOptional(planId);
            if (!planOptional.isPresent()) {
                return CommonResult.validateFailed("不存在，该出诊计划！");
            }
            timePeriod = correctTimePeriod(planOptional.get().getTime(), timePeriod);

            // 3. 全局唯一挂号单号
            long orderId = redisIdWorker.nextId("appointment");

            // 3. 执行 Lua 脚本（原子操作）
            String stockKey = STOCK_KEY_PREFIX + planId;
            String orderKey = ORDER_KEY_PREFIX + planId;

            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Arrays.asList(stockKey, orderKey),
                    String.valueOf(planId),
                    String.valueOf(cardId),
                    String.valueOf(orderId),
                    String.valueOf(accountId),
                    String.valueOf(timePeriod)
            );

            // 4. 判断结果
            if (result == null) {
                return CommonResult.failed("秒杀异常，请联系管理员！");
            }

            int r = result.intValue();
            if (r == 1) {
                // 库存不足：检查该用户是否实际已预约（Redis有记录但MySQL可能没有）
                // 如果MySQL也没有，则可能之前的秒杀全是脏数据
                return CommonResult.failed("号源已抢完！");
            } else if (r == 2) {
                // Redis 判定重复预约：需回查 MySQL 确认（防止 Stream 消费失败导致脏数据）
                if (!appointmentService.count(cardId, planId)) {
                    // MySQL 中无记录 → Redis 数据是脏的（之前秒杀成功但 Stream 消费者未写入MySQL）
                    LOGGER.warn("检测到Redis脏数据: planId={}, cardId={}, Redis判定重复但MySQL无记录，自动清理",
                            planId, cardId);
                    // 清理 Redis 中的重复标记
                    stringRedisTemplate.opsForSet().remove(orderKey, String.valueOf(cardId));
                    // 恢复库存（之前被误扣）
                    stringRedisTemplate.opsForValue().increment(stockKey, 1);
                    return CommonResult.failed("系统繁忙，请重新抢号");
                }
                return CommonResult.failed("您已预约过该号源！");
            }

            // 5. 秒杀成功：Redis已扣库存，Stream消息已由Lua脚本发出
            //    对标黑马点评架构：Controller只负责快速响应，Consumer异步完成MySQL写入
            LOGGER.info("秒杀成功 -> planId={}, cardId={}, orderId={}", planId, cardId, orderId);

            Map<String, Object> resultMap = new HashMap<>();
            // 使用 String 传递 orderId，防止 JavaScript Number 精度丢失（64位Long超过JS安全整数范围）
            resultMap.put("orderId", String.valueOf(orderId));
            resultMap.put("message", "号源已锁定，请尽快完成支付");
            resultMap.put("needPay", true);
            resultMap.put("expireMinutes", 15);
            return CommonResult.success(resultMap);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommonResult.failed("系统异常，请稍后再试！");
        } finally {
            // 6. 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 按出诊计划的上午/下午修正就诊时间段（服务端推算，防止前端硬编码导致时段与计划不符）
     * <p>
     * 上午计划的合法时段为 1~7，下午为 8~15；前端传值在合法区间内则保留（用户选了具体时段），
     * 否则取该时段的起始值兜底。
     *
     * @param planTime   出诊计划时间段：1 上午，2 下午
     * @param timePeriod 前端传入的就诊时间段
     * @return 修正后的就诊时间段
     */
    private Integer correctTimePeriod(Integer planTime, Integer timePeriod) {
        TimePeriodEnum period = TimePeriodEnum.AM.getTime().equals(planTime)
                ? TimePeriodEnum.AM : TimePeriodEnum.PM;

        if (timePeriod == null || timePeriod < period.getStart() || timePeriod > period.getEnd()) {
            return period.getStart();
        }
        return timePeriod;
    }

    /**
     * 重置指定号源的秒杀数据（仅用于测试/故障恢复）
     * <p>
     * 清理 Redis 中的库存和预约记录，并用指定库存重新初始化。
     * 注意：该操作不会影响 MySQL 中已有的预约记录。
     */
    @Operation(summary = "重置秒杀数据", description = "清理Redis秒杀缓存并用指定库存重新初始化，仅测试用")
    @PostMapping("/seckill/{planId}/reset")
    public CommonResult<String> resetSeckill(@PathVariable Long planId,
                                             @RequestParam(defaultValue = "30") Long stock) {
        String stockKey = STOCK_KEY_PREFIX + planId;
        String orderKey = ORDER_KEY_PREFIX + planId;

        // 删除旧的库存和预约记录
        stringRedisTemplate.delete(stockKey);
        stringRedisTemplate.delete(orderKey);

        // 重新设置库存
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));

        LOGGER.info("秒杀数据已重置: planId={}, stock={}", planId, stock);
        return CommonResult.success("秒杀数据已重置，库存=" + stock + "，planId=" + planId);
    }
}
