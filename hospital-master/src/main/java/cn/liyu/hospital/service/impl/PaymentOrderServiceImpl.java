package cn.liyu.hospital.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.liyu.hospital.common.api.CommonResult;
import com.github.pagehelper.PageHelper;
import cn.liyu.hospital.component.RedisIdWorker;
import cn.liyu.hospital.component.SimulatedPaymentGateway;
import cn.liyu.hospital.dto.PayStatusEnum;
import cn.liyu.hospital.dto.PaymentOrderDTO;
import cn.liyu.hospital.dto.PaymentStatusEnum;
import cn.liyu.hospital.entity.PaymentOrder;
import cn.liyu.hospital.entity.PaymentWallet;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.mapper.PaymentOrderMapper;
import cn.liyu.hospital.mapper.VisitAppointmentMapper;
import cn.liyu.hospital.service.IPaymentFlowService;
import cn.liyu.hospital.service.IPaymentOrderService;
import cn.liyu.hospital.service.IPaymentWalletService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 支付订单服务实现 —— 模拟微信支付 / 支付宝支付完整流程
 * <p>
 * 架构分层（等同于真实支付对接）：
 * <pre>
 *   Controller  →  PaymentOrderService  →  SimulatedPaymentGateway（模拟支付网关）
 *       ↑                ↑                           ↑
 *   接收HTTP请求      业务编排                  对接"支付平台"
 * </pre>
 * <p>
 * 真实场景替换方案：将 SimulatedPaymentGateway 替换为 WechatPayGateway / AlipayGateway，
 * 仅需修改网关层，Service 层代码无需变动。
 * <p>
 * 支付流程（与微信支付 JSAPI 一致）：
 * <ol>
 *   <li>创建支付订单（生成 payment_no，状态=UNPAID）</li>
 *   <li>调用支付网关「统一下单」→ 生成 prepay_id</li>
 *   <li>前端拿到 prepay_id，弹出支付确认页</li>
 *   <li>用户确认 → 调用支付网关「执行支付」→ 扣款</li>
 *   <li>更新订单状态 PAID → 记录流水 → 更新预约支付状态</li>
 * </ol>
 *
 * @author 医秒通
 */
@Service
public class PaymentOrderServiceImpl implements IPaymentOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentOrderServiceImpl.class);

    /** 支付单号前缀 */
    @Value("${payment.order-no-prefix:PAY}")
    private String orderNoPrefix;

    /** 挂号费（分），默认100分=1元 */
    @Value("${payment.appointment-fee:100}")
    private Long appointmentFee;

    /** 支付超时时间（分钟），默认15分钟 */
    @Value("${payment.expire-minutes:15}")
    private Integer expireMinutes;

    @Resource
    private PaymentOrderMapper orderMapper;

    @Resource
    private IPaymentWalletService walletService;

    @Resource
    private IPaymentFlowService flowService;

    @Resource
    private VisitAppointmentMapper appointmentMapper;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SimulatedPaymentGateway paymentGateway;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IVisitAppointmentService appointmentService;

    // ==================== 1. 创建支付订单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaymentOrder(Long accountId, Long appointmentId, Long amount) {
        // 1. 生成全局唯一ID（与秒杀订单一致，使用 RedisIdWorker）
        long paymentId = redisIdWorker.nextId("payment");

        // 2. 生成支付单号（PAY + 日期 + 随机串，等同于微信支付的 out_trade_no）
        String paymentNo = generatePaymentNo();

        // 3. 计算过期时间（默认15分钟）
        Date expireTime = DateUtil.offsetMinute(new Date(), expireMinutes);

        // 4. 构建支付订单
        PaymentOrder order = new PaymentOrder();
        order.setId(paymentId);
        order.setPaymentNo(paymentNo);
        order.setAccountId(accountId);
        order.setAppointmentId(appointmentId);
        order.setAmount(amount != null ? amount : appointmentFee);
        order.setStatus(PaymentStatusEnum.UNPAID.getStatus());
        order.setPayMethod("WALLET");
        order.setExpireTime(expireTime);
        order.setGmtCreate(new Date());
        order.setGmtModified(new Date());

        orderMapper.insert(order);

        // ========== 回写预约记录的支付关联 ==========
        // 秒杀场景：预约记录在 Controller 中已同步创建，此处回写 paymentId 建立关联
        // 普通预约场景：预约记录已存在，同样需要回写关联
        if (appointmentId != null) {
            VisitAppointment appointment = new VisitAppointment();
            appointment.setId(appointmentId);
            appointment.setPaymentId(paymentId);
            appointment.setPayStatus(PayStatusEnum.NOT_PAID.getStatus());
            appointment.setGmtModified(new Date());
            int rows = appointmentMapper.updateByPrimaryKeySelective(appointment);
            if (rows > 0) {
                LOGGER.info("预约记录已关联支付订单: appointmentId={}, paymentId={}", appointmentId, paymentId);
            } else {
                LOGGER.warn("预约记录关联支付订单失败(记录可能尚未写入): appointmentId={}, paymentId={}", appointmentId, paymentId);
            }
        }

        LOGGER.info("支付订单创建成功: paymentNo={}, amount={}分, expireTime={}",
                paymentNo, order.getAmount(), expireTime);
        return paymentId;
    }

    // ==================== 2. 执行支付（核心方法） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderDTO pay(Long paymentId, Long accountId) {
        // ========== 第一步：Redisson分布式锁（防并发支付） ==========
        String lockKey = "lock:payment:pay:" + paymentId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                LOGGER.warn("获取支付锁失败: paymentId={}", paymentId);
                throw new RuntimeException("系统繁忙，请稍后再试");
            }

            // ========== 第二步：查询并校验支付订单 ==========
            PaymentOrder order = orderMapper.selectByPrimaryKey(paymentId);
            if (order == null) {
                throw new RuntimeException("支付订单不存在");
            }
            // 状态机校验：只有「待支付」状态才能支付
            if (!Objects.equals(order.getStatus(), PaymentStatusEnum.UNPAID.getStatus())) {
                throw new RuntimeException("该订单状态为：" + PaymentStatusEnum.getDescByStatus(order.getStatus()));
            }
            // 过期校验
            if (order.getExpireTime().before(new Date())) {
                throw new RuntimeException("支付订单已过期，请重新下单");
            }
            // 账号校验
            if (!Objects.equals(order.getAccountId(), accountId)) {
                throw new RuntimeException("支付账号不匹配");
            }

            // ========== 第三步：状态改为 PAYING（防止重复支付） ==========
            order.setStatus(PaymentStatusEnum.PAYING.getStatus());
            order.setGmtModified(new Date());
            orderMapper.updateByPrimaryKeySelective(order);

            // ========== 第四步：查询钱包 ==========
            Optional<PaymentWallet> walletOpt = walletService.getByAccountId(accountId);
            if (!walletOpt.isPresent()) {
                markPayFailed(order, "钱包未初始化");
                throw new RuntimeException("钱包未初始化，请联系管理员");
            }
            PaymentWallet wallet = walletOpt.get();

            // ========== 第五步：调用支付网关「执行支付」 ==========
            // 真实场景：调用微信支付 SDK 扣款 / 支付宝 SDK 扣款
            // 模拟场景：校验余额 → 乐观锁扣款
            SimulatedPaymentGateway.PayResult payResult = paymentGateway.executePay(order, wallet);

            if (!payResult.isSuccess()) {
                // 支付失败（余额不足等）
                markPayFailed(order, payResult.getErrMsg());
                throw new RuntimeException(payResult.getErrMsg());
            }

            // ========== 第六步：乐观锁扣减钱包余额 ==========
            Long balanceBefore = wallet.getBalance();
            boolean deducted = walletService.deduct(wallet.getId(), order.getAmount(), wallet.getVersion());
            if (!deducted) {
                // 乐观锁冲突（并发扣款），回滚为支付失败
                markPayFailed(order, "扣款失败，请重试");
                throw new RuntimeException("扣款失败，请重试");
            }
            Long balanceAfter = balanceBefore - order.getAmount();

            // ========== 第七步：更新支付订单为「已支付」 ==========
            order.setStatus(PaymentStatusEnum.PAID.getStatus());
            order.setPayTime(new Date());
            order.setGmtModified(new Date());
            orderMapper.updateByPrimaryKeySelective(order);

            // ========== 第八步：记录支付流水（等同于微信支付的交易账单） ==========
            flowService.recordFlow(
                    paymentId, order.getPaymentNo(), accountId,
                    order.getAmount(), balanceBefore, balanceAfter,
                    1, "挂号支付"
            );

            // ========== 第九步：更新预约记录的支付状态（带重试，应对高并发下记录延迟写入） ==========
            if (order.getAppointmentId() != null) {
                boolean updated = false;
                for (int i = 0; i < 3; i++) {
                    VisitAppointment appointment = new VisitAppointment();
                    appointment.setId(order.getAppointmentId());
                    appointment.setPayStatus(PayStatusEnum.PAID.getStatus());
                    appointment.setPaymentId(paymentId);
                    appointment.setGmtModified(new Date());
                    int rows = appointmentMapper.updateByPrimaryKeySelective(appointment);
                    if (rows > 0) {
                        updated = true;
                        LOGGER.info("预约支付状态更新: appointmentId={}, payStatus=PAID", order.getAppointmentId());
                        break;
                    }
                    if (i < 2) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                if (!updated) {
                    LOGGER.error("更新预约支付状态失败（重试3次后）: appointmentId={}, paymentId={}，" +
                            "请人工核查", order.getAppointmentId(), paymentId);
                }
            }

            // ========== 第十步：返回支付结果 ==========
            LOGGER.info("支付成功: paymentNo={}, transactionId={}, amount={}分",
                    order.getPaymentNo(), payResult.getTransactionId(), order.getAmount());

            return convertToDTO(order);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("支付被中断");
        } catch (RuntimeException e) {
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 3. 退款 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderDTO refund(Long paymentId, String reason) {
        String lockKey = "lock:payment:refund:" + paymentId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                throw new RuntimeException("系统繁忙，请稍后再试");
            }

            // 查询订单
            PaymentOrder order = orderMapper.selectByPrimaryKey(paymentId);
            if (order == null) {
                throw new RuntimeException("支付订单不存在");
            }
            // 状态机校验：只有「已支付」才能退款
            if (!Objects.equals(order.getStatus(), PaymentStatusEnum.PAID.getStatus())) {
                throw new RuntimeException("该订单状态为：" + PaymentStatusEnum.getDescByStatus(order.getStatus()) + "，无法退款");
            }

            // ========== 调用支付网关「申请退款」==========
            SimulatedPaymentGateway.RefundResult refundResult = paymentGateway.applyRefund(order, reason);

            if (!refundResult.isSuccess()) {
                throw new RuntimeException(refundResult.getErrMsg() != null ? refundResult.getErrMsg() : "退款失败");
            }

            // ========== 退还余额 ==========
            Optional<PaymentWallet> walletOpt = walletService.getByAccountId(order.getAccountId());
            if (!walletOpt.isPresent()) {
                throw new RuntimeException("钱包不存在，退款失败");
            }
            PaymentWallet wallet = walletOpt.get();
            Long balanceBefore = wallet.getBalance();

            boolean added = walletService.addBalance(wallet.getId(), order.getAmount());
            if (!added) {
                throw new RuntimeException("退款入账失败");
            }
            Long balanceAfter = balanceBefore + order.getAmount();

            // ========== 更新支付订单 ==========
            order.setStatus(PaymentStatusEnum.REFUNDED.getStatus());
            order.setRefundAmount(order.getAmount());
            order.setRefundTime(new Date());
            order.setRefundReason(reason);
            order.setGmtModified(new Date());
            orderMapper.updateByPrimaryKeySelective(order);

            // ========== 记录退款流水 ==========
            flowService.recordFlow(
                    paymentId, order.getPaymentNo(), order.getAccountId(),
                    order.getAmount(), balanceBefore, balanceAfter,
                    2, "退款: " + (reason != null ? reason : "用户取消预约")
            );

            // ========== 更新预约记录 ==========
            if (order.getAppointmentId() != null) {
                VisitAppointment appointment = new VisitAppointment();
                appointment.setId(order.getAppointmentId());
                appointment.setPayStatus(PayStatusEnum.REFUNDED.getStatus());
                appointment.setGmtModified(new Date());
                appointmentMapper.updateByPrimaryKeySelective(appointment);
            }

            LOGGER.info("退款成功: paymentNo={}, refundId={}, amount={}分",
                    order.getPaymentNo(), refundResult.getRefundId(), order.getAmount());
            return convertToDTO(order);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("退款被中断");
        } catch (RuntimeException e) {
            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ==================== 4. 查询相关 ====================

    @Override
    public Optional<PaymentOrder> getById(Long paymentId) {
        return Optional.ofNullable(orderMapper.selectByPrimaryKey(paymentId));
    }

    @Override
    public PaymentOrderDTO getPaymentOrderDTO(Long paymentId) {
        PaymentOrder order = orderMapper.selectByPrimaryKey(paymentId);
        return order != null ? convertToDTO(order) : null;
    }

    @Override
    public List<PaymentOrderDTO> listByAccount(Long accountId, Integer status, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        List<PaymentOrder> orders = orderMapper.selectByAccountId(accountId, status);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(Long paymentId, Integer newStatus) {
        PaymentOrder order = new PaymentOrder();
        order.setId(paymentId);
        order.setStatus(newStatus);
        order.setGmtModified(new Date());
        return orderMapper.updateByPrimaryKeySelective(order) > 0;
    }

    @Override
    public boolean count(Long paymentId) {
        return orderMapper.selectByPrimaryKey(paymentId) != null;
    }

    @Override
    public PaymentOrderDTO getPaymentOrderDTOByAppointment(Long appointmentId) {
        PaymentOrder order = orderMapper.selectByAppointmentId(appointmentId);
        return order != null ? convertToDTO(order) : null;
    }

    // ==================== 5. 定时关闭过期订单 ====================

    @Override
    public int closeExpiredOrders() {
        List<PaymentOrder> expiredOrders = orderMapper.selectExpiredOrders(
                PaymentStatusEnum.UNPAID.getStatus(), new Date());
        if (expiredOrders == null || expiredOrders.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (PaymentOrder order : expiredOrders) {
            order.setStatus(PaymentStatusEnum.EXPIRED.getStatus());
            order.setGmtModified(new Date());
            orderMapper.updateByPrimaryKeySelective(order);
            count++;
            LOGGER.info("支付订单已过期关闭: paymentNo={}", order.getPaymentNo());

            // ========== 支付超时 → 取消关联预约 + 释放 Redis 号源库存 ==========
            if (order.getAppointmentId() != null) {
                try {
                    // 状态机校验：仅 WAITING→CANCEL 成功才释放，避免重复恢复
                    boolean cancelled = appointmentService.update(order.getAppointmentId(), 2);
                    if (cancelled) {
                        VisitAppointment app = appointmentService.getOptional(order.getAppointmentId()).orElse(null);
                        if (app != null) {
                            stringRedisTemplate.opsForValue().increment("seckill:stock:" + app.getPlanId(), 1);
                            stringRedisTemplate.opsForSet().remove("seckill:order:" + app.getPlanId(),
                                    String.valueOf(app.getCardId()));
                            LOGGER.info("支付超时释放号源: planId={}, cardId={}",
                                    app.getPlanId(), app.getCardId());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("支付超时取消预约失败: appointmentId={}", order.getAppointmentId(), e);
                }
            }
        }
        return count;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成支付单号（PAY + 日期 + 随机串）
     * 等同于微信支付的 out_trade_no 生成规则
     */
    private String generatePaymentNo() {
        String datePart = DateUtil.format(new Date(), "yyyyMMddHHmmss");
        String randomPart = RandomUtil.randomString(6);
        return orderNoPrefix + datePart + randomPart;
    }

    /**
     * 标记支付失败
     */
    private void markPayFailed(PaymentOrder order, String errMsg) {
        order.setStatus(PaymentStatusEnum.FAILED.getStatus());
        order.setGmtModified(new Date());
        orderMapper.updateByPrimaryKeySelective(order);
        LOGGER.warn("支付失败: paymentNo={}, reason={}", order.getPaymentNo(), errMsg);
    }

    /**
     * Entity → DTO 转换
     */
    private PaymentOrderDTO convertToDTO(PaymentOrder order) {
        PaymentOrderDTO dto = new PaymentOrderDTO();
        dto.setPaymentId(order.getId());
        dto.setPaymentNo(order.getPaymentNo());
        dto.setAmount(order.getAmount());
        dto.setAmountYuan(String.format("%.2f", order.getAmount() / 100.0));
        dto.setStatus(order.getStatus());
        dto.setStatusDesc(PaymentStatusEnum.getDescByStatus(order.getStatus()));
        dto.setPayMethod(order.getPayMethod());
        dto.setPayTime(order.getPayTime());
        dto.setExpireTime(order.getExpireTime());
        dto.setAppointmentId(order.getAppointmentId());
        dto.setGmtCreate(order.getGmtCreate());
        return dto;
    }
}
