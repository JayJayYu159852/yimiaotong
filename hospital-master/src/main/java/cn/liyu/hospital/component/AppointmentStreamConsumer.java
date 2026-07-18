package cn.liyu.hospital.component;

import cn.hutool.core.date.DateUtil;
import cn.liyu.hospital.dto.AppointmentMessageDTO;
import cn.liyu.hospital.dto.VisitPlanDTO;
import cn.liyu.hospital.entity.PaymentOrder;
import cn.liyu.hospital.entity.UserMedicalCard;
import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.mapper.PaymentOrderMapper;
import cn.liyu.hospital.mapper.VisitAppointmentMapper;
import cn.liyu.hospital.service.IPaymentOrderService;
import cn.liyu.hospital.service.IUserMedicalCardService;
import cn.liyu.hospital.service.IVisitAppointmentService;
import cn.liyu.hospital.service.IVisitPlanService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream 异步下单消费者（对标黑马点评 VoucherOrderHandler）
 * <p>
 * 消费者是 MySQL 写入的唯一入口，Controller 只负责 Redis 原子校验+快速响应。
 * 职责：
 * - 幂等校验：预约记录已存在则跳过创建，仅回填兜底
 * - 创建预约记录 + 支付订单（消费者是MySQL主力写入者）
 * - PendingList 兜底：处理未被ACK的消息，保证最终一致性
 * <p>
 * 架构关键：与 VisitSeckillController 使用相同锁Key，确保生产者/消费者互斥。
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Component
public class AppointmentStreamConsumer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentStreamConsumer.class);

    private static final String STREAM_KEY = "stream.appointment.orders";
    private static final String GROUP = "appointment-consumer-group";
    private static final String CONSUMER = "consumer-1";

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Resource
    private IPaymentOrderService paymentOrderService;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private VisitAppointmentMapper visitAppointmentMapper;

    @Resource
    private IUserMedicalCardService userMedicalCardService;

    @Resource
    private IVisitPlanService planService;

    @Resource
    private WebSocketServer webSocketServer;

    /**
     * MQ 生产者（@Profile("rabbitmq") 条件 Bean，未激活时为 null，调用前判空）
     */
    @Autowired(required = false)
    private MqProducerService mqProducerService;

    @Override
    public void run(String... args) {
        initStreamGroup();
        executor.submit(this::consumeLoop);
        LOGGER.info("Redis Stream 消费者已启动 -> stream: {}", STREAM_KEY);
    }

    /**
     * 应用关闭时优雅停止消费者线程，防止消息处理中断
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Redis Stream 消费者已关闭");
    }

    /**
     * 初始化 Stream 消费者组（幂等）
     * <p>
     * 仅忽略 BUSYGROUP（组已存在）异常；其他异常（连接失败等）必须打错误日志——
     * 否则启动期创建失败被静默吞掉后，消费循环将无限报 NOGROUP 且难以排查
     */
    private void initStreamGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP);
            LOGGER.info("Stream 消费者组创建成功 -> stream: {}, group: {}", STREAM_KEY, GROUP);
        } catch (Exception e) {
            if (isBusyGroup(e)) {
                // 消费者组已存在，属正常情况
                LOGGER.info("Stream 消费者组已存在 -> stream: {}, group: {}", STREAM_KEY, GROUP);
            } else {
                LOGGER.error("Stream 消费者组创建失败 -> stream: {}, group: {}", STREAM_KEY, GROUP, e);
            }
        }
    }

    /**
     * 主消费循环（对标 hmdp 的 VoucherOrderHandler.run()）
     */
    private void consumeLoop() {
        while (running.get()) {
            try {
                // 1. 从 Stream 读取一条未处理的消息（阻塞2秒）
                List<MapRecord<String, Object, Object>> records =
                        stringRedisTemplate.opsForStream().read(
                                Consumer.from(GROUP, CONSUMER),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                        );

                // 2. 无新消息，继续等待
                if (records == null || records.isEmpty()) {
                    continue;
                }

                // 3. 解析消息
                MapRecord<String, Object, Object> record = records.get(0);
                Map<Object, Object> valueMap = record.getValue();

                // 4. 处理订单
                handleOrder(valueMap);

                // 5. ACK 确认
                stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());

            } catch (Exception e) {
                // NOGROUP：stream 或消费者组不存在（如 Redis 数据迁移/被清理）→ 自愈重建后继续消费
                if (isNoGroup(e)) {
                    LOGGER.warn("Stream或消费者组不存在，尝试自愈重建 -> stream: {}", STREAM_KEY);
                    initStreamGroup();
                    sleepQuietly(2000);
                    continue;
                }
                LOGGER.error("Stream消费异常", e);
                // 异常时处理 Pending List
                handlePendingList();
            }
        }
    }

    /**
     * 判断异常是否为 BUSYGROUP（消费者组已存在，创建时的正常幂等情况）
     */
    private boolean isBusyGroup(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("BUSYGROUP");
    }

    /**
     * 判断异常是否为 NOGROUP（stream 或消费者组不存在，可通过重建自愈）
     */
    private boolean isNoGroup(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("NOGROUP");
    }

    /**
     * 静默休眠（自愈重试间隔，避免异常时空转刷屏）
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 处理秒杀订单（对标黑马点评 VoucherOrderHandler）
     * <p>
     * 消费者是MySQL写入的唯一入口，负责创建预约记录和支付订单。
     * 使用与Controller相同的分布式锁，确保同一用户同一号源不会并发创建。
     */
    private void handleOrder(Map<Object, Object> valueMap) {
        Long planId = toLong(valueMap.get("planId"));
        Long cardId = toLong(valueMap.get("cardId"));
        Long orderId = toLong(valueMap.get("orderId"));
        Long accountId = toLong(valueMap.get("accountId"));
        Integer timePeriod = toInt(valueMap.get("timePeriod"));

        // 使用与Controller相同的锁Key，确保Controller和Consumer互斥
        String lockKey = "lock:seckill:order:" + planId + ":" + cardId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                LOGGER.warn("获取锁失败，跳过此条 -> orderId={}", orderId);
                return;
            }

            // 幂等性保护：检查预约记录是否已存在
            if (appointmentService.count(cardId, planId)) {
                LOGGER.info("预约记录已存在，跳过创建 -> orderId={}", orderId);
                // 兜底：确保支付订单关联完整
                ensurePaymentOrderExists(orderId, accountId);
                return;
            }

            // ========== 创建预约记录 ==========
            LOGGER.info("Stream消费者创建预约记录 -> orderId={}, planId={}, cardId={}",
                    orderId, planId, cardId);

            VisitAppointment appointment = new VisitAppointment();
            appointment.setId(orderId);
            appointment.setPlanId(planId);
            appointment.setCardId(cardId);
            appointment.setAccountId(accountId);
            appointment.setTimePeriod(timePeriod);
            appointment.setStatus(0);      // 待就诊
            appointment.setPayStatus(0);   // 未支付
            appointment.setGmtCreate(new java.util.Date());
            appointment.setGmtModified(new java.util.Date());

            appointmentService.insertStreamOrder(appointment);
            LOGGER.info("Stream消费者写入预约成功 -> orderId={}", orderId);

            // ========== WebSocket 实时推送管理端（对标苍穹外卖来单提醒 type=1） ==========
            pushAppointmentNotice(orderId, cardId, planId);

            // ========== 就诊提醒延迟消息（DLX 死信队列，就诊前一天 20:00 触达） ==========
            sendVisitReminder(orderId, planId, cardId, accountId, timePeriod);

            // ========== 创建支付订单 ==========
            createPaymentOrderForAppointment(accountId, orderId);

        } catch (DataIntegrityViolationException e) {
            // 数据完整性异常（外键约束、唯一约束等）→ 不可重试，记录错误后ACK丢弃
            LOGGER.error("订单数据异常，已丢弃(不可重试): orderId={}, planId={}, cardId={}, error={}",
                    orderId, planId, cardId, e.getMessage());
        } catch (Exception e) {
            // 其他异常（数据库连接、超时等）→ 可重试，重新抛出以阻止ACK，进入PendingList等待重试
            LOGGER.error("订单处理失败(可重试) -> orderId={}", orderId, e);
            throw new RuntimeException("订单处理失败，进入PendingList重试", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * WebSocket 推送预约成功提醒（type=1）
     * <p>
     * 查询患者/医生姓名组装提醒文案；查询失败降级为通用文案，任何异常不影响下单主流程。
     *
     * @param orderId 预约编号
     * @param cardId  就诊卡编号
     * @param planId  出诊计划编号
     */
    private void pushAppointmentNotice(Long orderId, Long cardId, Long planId) {
        try {
            String content;
            java.util.Optional<UserMedicalCard> cardOpt = userMedicalCardService.getOptional(cardId);
            java.util.Optional<VisitPlanDTO> planOpt = planService.getOptional(planId);
            if (cardOpt.isPresent() && planOpt.isPresent()) {
                content = String.format("患者%s预约了%s医生的门诊",
                        cardOpt.get().getName(), planOpt.get().getDoctorName());
            } else {
                // 降级文案：查询不到详情时仍保证提醒送达
                content = String.format("就诊卡%s预约成功（出诊计划%s）", cardId, planId);
            }
            webSocketServer.sendAppointmentMessage(1, orderId, content);
        } catch (Exception e) {
            LOGGER.error("WebSocket预约提醒推送失败(不影响下单) -> orderId={}", orderId, e);
        }
    }

    /**
     * 发送就诊提醒延迟消息（DLX 死信队列实现）
     * <p>
     * 预约创建成功后投递延迟消息，就诊前一天 20:00 到期自动转入提醒队列触达患者。
     * MQ 未启用（本地未激活 rabbitmq Profile）或已过提醒时点则跳过；任何异常不影响下单主流程。
     *
     * @param orderId    预约编号
     * @param planId     出诊计划编号
     * @param cardId     就诊卡编号
     * @param accountId  账号编号
     * @param timePeriod 时间段编号
     */
    private void sendVisitReminder(Long orderId, Long planId, Long cardId, Long accountId, Integer timePeriod) {
        if (mqProducerService == null) {
            // 本地环境未激活 rabbitmq Profile，跳过延迟提醒
            return;
        }
        try {
            java.util.Optional<UserMedicalCard> cardOpt = userMedicalCardService.getOptional(cardId);
            java.util.Optional<VisitPlanDTO> planOpt = planService.getOptional(planId);
            if (!cardOpt.isPresent() || !planOpt.isPresent()) {
                LOGGER.warn("就诊提醒跳过（就诊卡或出诊计划不存在）-> orderId={}", orderId);
                return;
            }
            UserMedicalCard card = cardOpt.get();
            VisitPlanDTO plan = planOpt.get();

            // 提醒时点：就诊前一天 20:00；已过时点（如当天挂当天号）不再提醒
            java.util.Date remindTime = DateUtil.offsetHour(
                    DateUtil.offsetDay(DateUtil.beginOfDay(plan.getDay()), -1), 20);
            long delayMs = remindTime.getTime() - System.currentTimeMillis();
            if (delayMs <= 0) {
                LOGGER.info("就诊提醒跳过（已过提醒时点）-> orderId={}, visitDate={}", orderId, plan.getDay());
                return;
            }

            AppointmentMessageDTO message = AppointmentMessageDTO.builder()
                    .appointmentId(orderId)
                    .planId(planId)
                    .cardId(cardId)
                    .accountId(accountId)
                    .timePeriod(timePeriod)
                    .patientName(card.getName())
                    .phoneNumber(card.getPhone())
                    .doctorName(plan.getDoctorName())
                    .visitDate(plan.getDay())
                    .build();
            mqProducerService.sendAppointmentReminder(message, delayMs);
        } catch (Exception e) {
            LOGGER.error("就诊提醒发送失败(不影响下单) -> orderId={}", orderId, e);
        }
    }

    /**
     * 确保支付订单存在（兜底修复）
     * <p>
     * 如果支付订单缺失（极端情况），则补建支付订单并回填关联关系。
     */
    private void ensurePaymentOrderExists(Long orderId, Long accountId) {
        try {
            // 查询已存在的支付订单
            PaymentOrder paymentOrder = paymentOrderMapper.selectByAppointmentId(orderId);
            if (paymentOrder != null) {
                // 支付订单已存在，检查预约记录关联是否完整
                VisitAppointment existing = visitAppointmentMapper.selectByPrimaryKey(orderId);
                if (existing != null && existing.getPaymentId() == null) {
                    existing.setPaymentId(paymentOrder.getId());
                    existing.setGmtModified(new java.util.Date());
                    visitAppointmentMapper.updateByPrimaryKeySelective(existing);
                    LOGGER.info("兜底回填paymentId成功: orderId={}, paymentId={}", orderId, paymentOrder.getId());
                }
                return;
            }

            // 支付订单不存在 → 补建
            LOGGER.warn("支付订单缺失，补建 -> orderId={}", orderId);
            createPaymentOrderForAppointment(accountId, orderId);
        } catch (Exception e) {
            LOGGER.warn("确保支付订单失败(非关键): orderId={}, error={}", orderId, e.getMessage());
        }
    }

    /**
     * 为预约创建支付订单
     */
    private void createPaymentOrderForAppointment(Long accountId, Long orderId) {
        try {
            Long paymentId = paymentOrderService.createPaymentOrder(accountId, orderId, null);
            LOGGER.info("Stream消费者创建支付订单成功: orderId={}, paymentId={}", orderId, paymentId);
        } catch (Exception e) {
            LOGGER.error("Stream消费者创建支付订单失败: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("支付订单创建失败", e);
        }
    }

    /**
     * 处理 Pending List（未确认消息兜底）
     */
    private void handlePendingList() {
        try {
            List<MapRecord<String, Object, Object>> records =
                    stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP, CONSUMER),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
                    );

            if (records != null && !records.isEmpty()) {
                MapRecord<String, Object, Object> record = records.get(0);
                handleOrder(record.getValue());
                stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
            }
        } catch (Exception e) {
            LOGGER.error("PendingList处理异常", e);
        }
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        return Long.valueOf(obj.toString());
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        return Integer.valueOf(obj.toString());
    }
}
