package cn.liyu.hospital.controller.user;

import cn.hutool.core.date.DateUtil;
import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.UserHolder;
import cn.liyu.hospital.component.MqProducerService;
import cn.liyu.hospital.component.RedisIdWorker;
import cn.liyu.hospital.component.WebSocketServer;
import cn.liyu.hospital.dto.*;
import cn.liyu.hospital.dto.param.VisitAppointmentParam;
import cn.liyu.hospital.entity.UserMedicalCard;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.dto.PayStatusEnum;
import cn.liyu.hospital.service.*;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.liyu.hospital.dto.AppointmentEnum.*;

/**
 * @author 医秒通
 */

@Tag(name = "用户端 · 预约挂号", description = "出诊预约接口")
@RestController
@RequestMapping("/visit")
public class VisitAppointmentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisitAppointmentController.class);

    @Resource
    private IPowerAccountService accountService;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Resource
    private IUserMedicalCardService userMedicalCardService;

    @Resource
    private IVisitPlanService planService;

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Autowired(required = false)
    private MqProducerService mqProducerService;

    @Resource
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IPaymentOrderService paymentOrderService;

    @Resource
    private IPaymentWalletService paymentWalletService;

    @Resource
    private WebSocketServer webSocketServer;

    private static final String RANK_SCORE_KEY = "doctor:rank:score";
    private static final String RANK_COUNT_KEY = "doctor:rank:count";

    /**
     * 已评分标记前缀（SETNX 防重复评分，对标黑马点评"一人一赞"思路；key 永久保留）
     */
    private static final String RATE_KEY_PREFIX = "rate:appointment:";

    /**
     * 秒杀库存 / 预约记录 Redis Key 前缀（与 VisitSeckillController 共用同一套 Key）
     */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String ORDER_KEY_PREFIX = "seckill:order:";

    /**
     * 秒杀 Lua 脚本（与 VisitSeckillController 共用同一套脚本）
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private RedisIdWorker redisIdWorker;

    @Operation(summary = "添加预约信息（统一走秒杀Lua脚本，与秒杀页共享库存）", description = "传入 预约参数对象（出诊编号、就诊卡号、账号编号）")
    @PostMapping("/appointment")
    public CommonResult<Map<String, Object>> insertAppointment(@RequestBody VisitAppointmentParam param) {

        // 0. 登录态校验：账号取自 JWT（不信任前端传值，对标秒杀控制器）
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return CommonResult.validateFailed("请先登录！");
        }
        Long accountId = user.getId();

        // 0.1 就诊卡校验
        if (!userMedicalCardService.countCardId(param.getCardId())) {
            return CommonResult.validateFailed("不存在，该就诊卡号！");
        }

        // 0.2 出诊计划校验 + 修正时间段
        Optional<VisitPlanDTO> planOptional = planService.getOptional(param.getPlanId());
        if (!planOptional.isPresent()) {
            return CommonResult.validateFailed("不存在，该出诊编号！");
        }
        Integer timePeriod = correctTimePeriod(planOptional.get().getTime(), param.getTimePeriod());

        // 1. Redisson 分布式锁（防重复提交，Key 与秒杀锁规则一致）
        String lockKey = "lock:appointment:" + param.getCardId() + ":" + param.getPlanId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                return CommonResult.failed("系统繁忙，请稍后再试！");
            }

            // 2. 双重检查：Redis 中是否已有预约记录（对标秒杀流程的脏数据清理）
            String orderKey = ORDER_KEY_PREFIX + param.getPlanId();
            Boolean alreadyDone = stringRedisTemplate.opsForSet().isMember(orderKey, String.valueOf(param.getCardId()));
            if (Boolean.TRUE.equals(alreadyDone)) {
                if (!appointmentService.count(param.getCardId(), param.getPlanId())) {
                    // Redis 脏数据自动清理
                    LOGGER.warn("检测到Redis脏数据: planId={}, cardId={}, 自动清理", param.getPlanId(), param.getCardId());
                    stringRedisTemplate.opsForSet().remove(orderKey, String.valueOf(param.getCardId()));
                    String stockKey = STOCK_KEY_PREFIX + param.getPlanId();
                    stringRedisTemplate.opsForValue().increment(stockKey, 1);
                    return CommonResult.failed("系统繁忙，请重新预约");
                }
                return CommonResult.failed("您已预约过该号源，请勿重复预约！");
            }

            // 3. 全局唯一挂号单号
            long orderId = redisIdWorker.nextId("appointment");

            // 4. 执行 Lua 脚本（原子操作：扣库存 + 防重复 + 发 Stream → 与秒杀页走同一套逻辑）
            String stockKey = STOCK_KEY_PREFIX + param.getPlanId();
            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Arrays.asList(stockKey, orderKey),
                    String.valueOf(param.getPlanId()),
                    String.valueOf(param.getCardId()),
                    String.valueOf(orderId),
                    String.valueOf(accountId),
                    String.valueOf(timePeriod)
            );

            if (result == null) {
                return CommonResult.failed("系统异常，请联系管理员！");
            }

            int r = result.intValue();
            if (r == 1) {
                return CommonResult.failed("号源已抢完！");
            } else if (r == 2) {
                return CommonResult.failed("您已预约过该号源！");
            }

            // 5. 挂号成功：MySQL写入 + 支付订单 + 通知由 Stream 消费者异步完成
            LOGGER.info("挂号成功(统一入口) -> planId={}, cardId={}, orderId={}", param.getPlanId(), param.getCardId(), orderId);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("orderId", String.valueOf(orderId));
            resultMap.put("message", "号源已锁定，请尽快完成支付");
            resultMap.put("needPay", true);
            resultMap.put("expireMinutes", 15);
            return CommonResult.success(resultMap);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CommonResult.failed("系统异常，请稍后再试！");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Operation(summary = "判断是否已预约", description = "传入 出诊编号、就诊卡号")
    @GetMapping("/appointment/check")
    public CommonResult<Boolean> checkAppointment(@RequestParam Long cardId, @RequestParam Long planId) {

        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡号！");
        }

        if (!planService.count(planId)) {
            return CommonResult.validateFailed("不存在，该出诊编号！");
        }

        return CommonResult.success(appointmentService.count(cardId, planId));
    }

    @Operation(summary = "修改预约状态：取消", description = "传入 预约编号")
    @PutMapping("/appointment/cancel/{id}")
    public CommonResult<String> cancelAppointment(@PathVariable Long id) {

        return updateAppointmentStatus(id, CANCEL.getStatus(), AppointmentMessageDTO.MessageType.APPOINTMENT_CANCEL);
    }

    @Operation(summary = "查找挂号记录", description = "传入就诊卡编号、挂号状态。不传cardId时默认按当前登录用户过滤")
    @GetMapping("/appointment/search")
    public CommonResult<CommonPage<VisitAppointmentDTO>> searchAppointment(@RequestParam(required = false) Long cardId,
                                                                           @RequestParam(required = false) Integer status,
                                                                           @RequestParam Integer pageNum,
                                                                           @RequestParam Integer pageSize) {

        // 安全防护：如果没传 cardId，则按当前登录用户的 accountId 过滤，防止泄露全量数据
        List<VisitAppointment> list;
        if (cardId != null) {
            list = appointmentService.list(cardId, status, pageNum, pageSize);
        } else {
            UserDTO currentUser = UserHolder.getUser();
            if (currentUser == null) {
                return CommonResult.failed("请先登录");
            }
            list = appointmentService.listByAccountId(currentUser.getId(), status, pageNum, pageSize);
        }

        // 先取分页信息，再转DTO（list实际是Page类型，stream转换后会丢失分页信息）
        PageInfo<VisitAppointment> pageInfo = new PageInfo<>(list);
        List<VisitAppointmentDTO> dtoList = list.stream()
                .map(appointmentService::convertToDTO)
                .collect(Collectors.toList());

        // 手工构建CommonPage，保留真实分页数据
        CommonPage<VisitAppointmentDTO> page = new CommonPage<>();
        page.setPageNum(pageInfo.getPageNum());
        page.setPageSize(pageInfo.getPageSize());
        page.setTotalPage(pageInfo.getPages());
        page.setTotal(pageInfo.getTotal());
        page.setList(dtoList);
        return CommonResult.success(page);
    }

    @Operation(summary = "获取所有挂号记录", description = "传入就诊卡编号、账号编号")
    @GetMapping("/appointment/all")
    public CommonResult<CommonPage<VisitAppointmentWithQueueDTO>> listAllAppointment(@RequestParam Long cardId,
                                                                                     @RequestParam Long accountId,
                                                                                     @RequestParam Integer pageNum,
                                                                                     @RequestParam Integer pageSize) {

        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        if (!accountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        return CommonResult.success(CommonPage.restPage(appointmentService.listAllAppointment(cardId, accountId, pageNum, pageSize)));
    }

    @Operation(summary = "获取失信记录详情", description = "传入预约编号")
    @GetMapping("/appointment/miss/details")
    public CommonResult<VisitAppointmentWithQueueDTO> listAllAppointment(@RequestParam Long appointmentId) {

        if (!appointmentService.count(appointmentId)) {
            return CommonResult.validateFailed("不存在，该预约编号！");
        }

        return CommonResult.success(appointmentService.getAppointmentDetails(appointmentId));
    }

    @Operation(summary = "获取就诊记录列表", description = "传入就诊卡编号")
    @GetMapping("/appointment/list")
    public CommonResult<CommonPage<VisitAppointmentDTO>> listAppointment(@RequestParam Long cardId, @RequestParam Integer pageNum,
                                                                         @RequestParam Integer pageSize) {

        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        return CommonResult.success(CommonPage.restPage(appointmentService.listNormalAppointment(cardId, pageNum, pageSize)));
    }

    @Operation(summary = "查看就诊记录详情", description = "传入就诊卡编号")
    @GetMapping("/appointment/details")
    public CommonResult<VisitAppointmentWithCaseDTO> getAppointmentDetails(@RequestParam Long appointmentId) {

        if (!appointmentService.count(appointmentId)) {
            return CommonResult.validateFailed("不存在，该预约编号！");
        }

        return CommonResult.success(appointmentService.getVisitAppointmentWithCaseDTO(appointmentId));
    }

    @Operation(summary = "查看当天排队信息", description = "传入就诊卡编号、账号编号")
    @GetMapping("/appointment/today")
    public CommonResult<CommonPage<VisitAppointmentQueueDTO>> getTodayAppointment(@RequestParam Long cardId,
                                                                                  @RequestParam Long accountId,
                                                                                  @RequestParam String date) {

        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }

        if (!accountService.count(accountId)) {
            return CommonResult.validateFailed("不存在，该账号编号！");
        }

        return CommonResult.success(CommonPage.restPage(appointmentService.listTodayQueue(DateUtil.parseDate(date), cardId, accountId)));
    }

    @Operation(summary = "查看获取预约诊室名称", description = "传入医生编号、日期、时间段（上午：1、下午：2）")
    @GetMapping("/appointment/clinic")
    public CommonResult<String> getClinicName(@RequestParam Long doctorId, @RequestParam String date,
                                      @RequestParam Integer time) {

        if (!hospitalDoctorService.count(doctorId)) {
            return CommonResult.validateFailed("不存在，该医生编号");
        }

        Date day = DateUtil.parse(date);

        return CommonResult.success(appointmentService.getClinicName(doctorId, time, day));
    }

    // ==================== ⭐ 就诊后评分 ====================

    @Operation(summary = "就诊完成后对医生评分", description = "传入预约编号、评分（1-5星）")
    @PostMapping("/appointment/rate/{id}")
    public CommonResult<String> rateDoctor(@PathVariable Long id, @RequestParam Integer score) {
        if (!appointmentService.count(id)) {
            return CommonResult.validateFailed("不存在，该预约编号！");
        }
        if (score < 1 || score > 5) {
            return CommonResult.validateFailed("评分必须在1-5之间！");
        }

        // 获取预约记录中的医生信息
        Optional<VisitAppointment> appointmentOpt = appointmentService.getOptional(id);
        if (!appointmentOpt.isPresent()) {
            return CommonResult.failed("预约记录不存在！");
        }
        VisitAppointment appointment = appointmentOpt.get();

        // 本人校验：只有该预约的所有者才能评分
        Long currentUserId = UserHolder.getUser().getId();
        if (!appointment.getAccountId().equals(currentUserId)) {
            return CommonResult.failed("只能对自己的预约进行评分！");
        }

        // 只有完成的预约才能评分
        if (!FINISH.getStatus().equals(appointment.getStatus())) {
            return CommonResult.failed("只有已完成就诊的预约才能评分！");
        }

        // 获取医生编号
        Optional<VisitPlanDTO> planOpt = planService.getOptional(appointment.getPlanId());
        if (!planOpt.isPresent()) {
            return CommonResult.failed("出诊计划不存在！");
        }

        Long doctorId = planOpt.get().getDoctorId();

        // 防重复评分：SETNX 原子占位（对标黑马点评"一人一赞"），占位失败说明已评过
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(RATE_KEY_PREFIX + id, String.valueOf(score));
        if (!Boolean.TRUE.equals(first)) {
            return CommonResult.failed("该预约已评价过，请勿重复评分！");
        }

        // ZINCRBY doctor:rank:score {score} {doctorId}
        stringRedisTemplate.opsForZSet().incrementScore(RANK_SCORE_KEY, String.valueOf(doctorId), score);
        // ZINCRBY doctor:rank:count 1 {doctorId}
        stringRedisTemplate.opsForZSet().incrementScore(RANK_COUNT_KEY, String.valueOf(doctorId), 1);

        LOGGER.info("评分成功: appointmentId={}, doctorId={}, score={}", id, doctorId, score);
        return CommonResult.success("评分成功！");
    }

    @Operation(summary = "查询预约是否已评分", description = "传入预约编号，用于前端展示已评分状态")
    @GetMapping("/appointment/rate/status/{id}")
    public CommonResult<Boolean> getRateStatus(@PathVariable Long id) {
        return CommonResult.success(
                Boolean.TRUE.equals(stringRedisTemplate.hasKey(RATE_KEY_PREFIX + id)));
    }

    /**
     * 更新预订状态
     *
     * @param id     预订编号
     * @param status 状态：1 失约，2 取消， 3 完成
     * @param mqType MQ消息类型（取消/失约时发送，完成时传null）
     * @return 更新情况
     */
    private CommonResult<String> updateAppointmentStatus(Long id, Integer status, AppointmentMessageDTO.MessageType mqType) {
        Optional<VisitAppointment> appointmentOpt = appointmentService.getOptional(id);
        if (!appointmentOpt.isPresent()) {
            return CommonResult.validateFailed("不存在，该预约编号！");
        }

        // 状态机校验：仅"待就诊"可流转到 失约/取消/完成
        VisitAppointment appointment = appointmentOpt.get();
        if (!WAITING.getStatus().equals(appointment.getStatus())) {
            return CommonResult.validateFailed("当前预约状态不允许此操作（仅待就诊的预约可操作）！");
        }

        // ========== 取消预约时自动退款 ==========
        if (AppointmentEnum.CANCEL.getStatus().equals(status)) {
            // 如果已支付，则自动退款
            if (appointment.getPaymentId() != null
                    && PayStatusEnum.PAID.getStatus().equals(appointment.getPayStatus())) {
                try {
                    paymentOrderService.refund(appointment.getPaymentId(), "用户取消预约");
                    LOGGER.info("取消预约自动退款成功: appointmentId={}, paymentId={}", id, appointment.getPaymentId());
                } catch (Exception e) {
                    LOGGER.error("取消预约自动退款失败: appointmentId={}, error={}", id, e.getMessage(), e);
                }
            }
        }

        if (appointmentService.update(id, status)) {
            // ========== 发送状态变更 MQ 消息（取消/失约时） ==========
            if (mqType != null) {
                try {
                    sendAppointmentStatusMqMessage(id, mqType);
                } catch (Exception e) {
                    LOGGER.error("MQ状态变更消息发送失败: appointmentId={}, mqType={}", id, mqType, e);
                }
            }

            // ========== 取消预约释放 Redis 号源库存（与秒杀入口共用同一套 Key） ==========
            if (AppointmentEnum.CANCEL.getStatus().equals(status)) {
                try {
                    stringRedisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + appointment.getPlanId(), 1);
                    stringRedisTemplate.opsForSet().remove(ORDER_KEY_PREFIX + appointment.getPlanId(),
                            String.valueOf(appointment.getCardId()));
                    LOGGER.info("取消预约释放号源: planId={}, cardId={}",
                            appointment.getPlanId(), appointment.getCardId());
                } catch (Exception e) {
                    LOGGER.error("取消预约释放Redis号源失败: appointmentId={}", id, e);
                }
            }

            return CommonResult.success();
        }

        return CommonResult.failed();
    }

    /**
     * 发送预约状态变更 MQ 消息
     *
     * @param appointmentId 预约编号
     * @param messageType   消息类型（APPOINTMENT_CANCEL / APPOINTMENT_MISS）
     */
    private void sendAppointmentStatusMqMessage(Long appointmentId, AppointmentMessageDTO.MessageType messageType) {
        // 1. 获取预约信息
        Optional<VisitAppointment> appointmentOpt = appointmentService.getOptional(appointmentId);
        if (!appointmentOpt.isPresent()) {
            return;
        }
        VisitAppointment appointment = appointmentOpt.get();

        // 2. 获取患者信息
        Optional<UserMedicalCard> cardOpt = userMedicalCardService.getOptional(appointment.getCardId());
        if (!cardOpt.isPresent()) {
            return;
        }
        UserMedicalCard card = cardOpt.get();

        // 3. 获取出诊计划（VisitPlanDTO 已包含 doctorName/clinicName）
        Optional<VisitPlanDTO> planOpt = planService.getOptional(appointment.getPlanId());

        // 4. 构建消息
        AppointmentMessageDTO.AppointmentMessageDTOBuilder builder = AppointmentMessageDTO.builder()
                .messageType(messageType.name())
                .appointmentId(appointmentId)
                .planId(appointment.getPlanId())
                .cardId(appointment.getCardId())
                .accountId(appointment.getAccountId())
                .patientName(card.getName())
                .phoneNumber(card.getPhone());

        if (planOpt.isPresent()) {
            VisitPlanDTO plan = planOpt.get();
            builder.doctorName(plan.getDoctorName())
                    .visitDate(plan.getDay());
        }

        // 5. 发送广播消息（Fanout）
        if (mqProducerService != null) {
            mqProducerService.sendStatusChange(builder.build());
        }

        // 6. WebSocket 实时推送管理端（对标苍穹外卖催单提醒 type=2，仅取消预约时推送）
        if (AppointmentMessageDTO.MessageType.APPOINTMENT_CANCEL == messageType) {
            String doctorName = planOpt.map(VisitPlanDTO::getDoctorName).orElse("未知");
            webSocketServer.sendAppointmentMessage(2, appointmentId,
                    String.format("患者%s取消了%s医生的预约", card.getName(), doctorName));
        }
    }

    // ==================== 我的信用 ====================

    @Operation(summary = "查看我的当月信用", description = "传入就诊卡编号，查看当月爽约/取消/完成统计")
    @GetMapping("/appointment/credit/current")
    public CommonResult<UserCreditDTO> getCurrentCredit(@RequestParam Long cardId) {
        Long accountId = UserHolder.getUser().getId();
        if (!userMedicalCardService.countCardId(cardId)) {
            return CommonResult.validateFailed("不存在，该就诊卡编号！");
        }
        UserCreditDTO credit = appointmentService.getCurrentCredit(accountId, cardId);
        return credit != null ? CommonResult.success(credit) : CommonResult.failed("暂无信用记录");
    }

    /**
     * 按出诊计划的上午/下午修正就诊时间段（与 VisitSeckillController 共用同一逻辑）
     * <p>
     * 上午计划的合法时段为 1~7，下午为 8~15；前端传值在合法区间内则保留，
     * 否则取该时段的起始值兜底。
     */
    private Integer correctTimePeriod(Integer planTime, Integer timePeriod) {
        TimePeriodEnum period = TimePeriodEnum.AM.getTime().equals(planTime)
                ? TimePeriodEnum.AM : TimePeriodEnum.PM;

        if (timePeriod == null || timePeriod < period.getStart() || timePeriod > period.getEnd()) {
            return period.getStart();
        }
        return timePeriod;
    }
}
