package cn.liyu.hospital.component;

import cn.hutool.core.date.DateUtil;
import cn.liyu.hospital.config.RabbitMQConfig;
import cn.liyu.hospital.dto.AppointmentMessageDTO;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 预约通知消息消费者（RabbitMQ Listener）
 * <p>
 * 负责消费三个队列的消息：
 * 1. 预约成功通知 — 发送 SMS 短信确认
 * 2. 就诊提醒通知 — 延迟队列过期后触发，发送提醒 SMS
 * 3. 状态变更通知 — Fanout 广播消费，发送取消/失约 SMS
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Profile("rabbitmq")
@Component
public class AppointmentNotificationReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentNotificationReceiver.class);

    @Resource
    private AliSendSmsComponent aliSendSmsComponent;

    // ==================== 场景1：预约成功即时通知 ====================

    /**
     * 监听预约成功通知队列
     * <p>
     * 收到消息后，调用阿里云短信服务，向患者手机发送预约成功确认短信。
     * 使用手动确认（MANUAL ACK），防止消息丢失。
     */
    @RabbitListener(queues = RabbitMQConfig.APPOINTMENT_NOTIFY_QUEUE)
    public void handleAppointmentNotify(AppointmentMessageDTO message, Channel channel, Message amqpMessage) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            LOGGER.info("收到预约成功通知 -> 预约ID: {}, 患者: {}, 医生: {}",
                    message.getAppointmentId(), message.getPatientName(),
                    message.getDoctorName());

            // 1. 组装短信内容
            String smsContent = buildAppointmentSmsContent(message);

            // 2. 发送短信通知（通过阿里云短信服务）
            if (message.getPhoneNumber() != null && !message.getPhoneNumber().isEmpty()) {
                // 注意：实际发送需要对应的短信模板，这里使用日志模拟
                // aliSendSmsComponent.sendRegisterCode(message.getPhoneNumber(), smsContent);
                LOGGER.info("【模拟短信发送】手机号: {}, 内容: {}", message.getPhoneNumber(), smsContent);
            } else {
                LOGGER.warn("手机号为空，跳过短信发送 -> 预约ID: {}", message.getAppointmentId());
            }

            // 3. 手动确认消息
            channel.basicAck(deliveryTag, false);
            LOGGER.info("预约成功通知处理完成 (ACK) -> 预约ID: {}", message.getAppointmentId());

        } catch (Exception e) {
            LOGGER.error("预约成功通知处理失败 -> 预约ID: {}", message.getAppointmentId(), e);
            try {
                // 消费失败，拒绝消息且不重新入队（避免毒消息死循环）
                // 应对策略：记录到数据库失败表，后续人工/定时任务补偿
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                LOGGER.error("消息拒绝失败", ex);
            }
        }
    }

    // ==================== 场景2：就诊提醒（延迟队列到期后的消费） ====================

    /**
     * 监听就诊提醒队列（延迟队列的消息过期后自动进入此队列）
     * <p>
     * 典型延迟配置：
     * - 预约时间前 24 小时提醒 → delayMs = 86400000
     * - 预约时间前 2 小时提醒  → delayMs = 7200000
     * <p>
     * 消息到达此队列时，说明延迟时间已到，应立即发送提醒短信。
     */
    @RabbitListener(queues = RabbitMQConfig.REMINDER_QUEUE)
    public void handleAppointmentReminder(AppointmentMessageDTO message, Channel channel, Message amqpMessage) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            String visitDateStr = message.getVisitDate() != null
                    ? DateUtil.format(message.getVisitDate(), "yyyy年MM月dd日") : "未知日期";

            LOGGER.info("收到就诊提醒（延迟消息已到期）-> 预约ID: {}, 患者: {}, 就诊日期: {}, 医生: {}",
                    message.getAppointmentId(), message.getPatientName(),
                    visitDateStr, message.getDoctorName());

            // 1. 组装提醒短信
            String remindContent = String.format(
                    "【医院挂号提醒】%s您好，您预约了%s（%s）%s医生的门诊，请按时就诊。如有变动请及时取消预约。",
                    message.getPatientName(),
                    visitDateStr,
                    getTimePeriodDesc(message.getTimePeriod()),
                    message.getDoctorName()
            );

            // 2. 发送短信
            if (message.getPhoneNumber() != null && !message.getPhoneNumber().isEmpty()) {
                LOGGER.info("【模拟短信发送-就诊提醒】手机号: {}, 内容: {}", message.getPhoneNumber(), remindContent);
            }

            // 3. 手动确认
            channel.basicAck(deliveryTag, false);
            LOGGER.info("就诊提醒处理完成 (ACK) -> 预约ID: {}", message.getAppointmentId());

        } catch (Exception e) {
            LOGGER.error("就诊提醒处理失败 -> 预约ID: {}", message.getAppointmentId(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                LOGGER.error("消息拒绝失败", ex);
            }
        }
    }

    // ==================== 场景3：状态变更通知（Fanout 广播消费） ====================

    /**
     * 监听状态变更通知队列（SMS 渠道）
     * <p>
     * 当预约被取消、失约等状态变更时，Fanout 交换机向所有绑定队列广播消息。
     * 此监听器处理 SMS 通知渠道。如需增加邮件通知，新增一个队列 + 监听器即可。
     */
    @RabbitListener(queues = RabbitMQConfig.STATUS_SMS_QUEUE)
    public void handleStatusChange(AppointmentMessageDTO message, Channel channel, Message amqpMessage) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            String actionDesc = getStatusActionDesc(message.getMessageType());

            LOGGER.info("收到状态变更通知 -> 预约ID: {}, 类型: {}, 患者: {}",
                    message.getAppointmentId(), actionDesc, message.getPatientName());

            // 1. 组装通知短信
            String smsContent = String.format(
                    "【医院挂号通知】%s您好，您的预约挂号（%s医生）已%s。如有疑问请致电医院。",
                    message.getPatientName(),
                    message.getDoctorName(),
                    actionDesc
            );

            // 2. 发送短信
            if (message.getPhoneNumber() != null && !message.getPhoneNumber().isEmpty()) {
                LOGGER.info("【模拟短信发送-状态变更】手机号: {}, 内容: {}", message.getPhoneNumber(), smsContent);
            }

            // 3. 手动确认
            channel.basicAck(deliveryTag, false);
            LOGGER.info("状态变更通知处理完成 (ACK) -> 预约ID: {}", message.getAppointmentId());

        } catch (Exception e) {
            LOGGER.error("状态变更通知处理失败 -> 预约ID: {}", message.getAppointmentId(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                LOGGER.error("消息拒绝失败", ex);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 组装预约成功短信内容
     */
    private String buildAppointmentSmsContent(AppointmentMessageDTO message) {
        String dateStr = message.getVisitDate() != null
                ? DateUtil.format(message.getVisitDate(), "yyyy年MM月dd日") : "未知日期";

        return String.format(
                "【医院预约挂号】%s您好，您已成功预约%s（%s）%s医生的门诊。请携带就诊卡按时就诊。",
                message.getPatientName(),
                dateStr,
                getTimePeriodDesc(message.getTimePeriod()),
                message.getDoctorName()
        );
    }

    /**
     * 获取时间段描述
     */
    private String getTimePeriodDesc(Integer timePeriod) {
        if (timePeriod == null) return "未知时段";

        String[] periodDesc = {
                "8:30-9:00", "9:00-9:30", "9:30-10:00", "10:00-10:30",
                "10:30-11:00", "11:00-11:30", "11:30-12:00",
                "14:00-14:30", "14:30-15:00", "15:00-15:30",
                "15:30-16:00", "16:00-16:30", "16:30-17:00", "17:00-17:30"
        };

        if (timePeriod >= 1 && timePeriod <= periodDesc.length) {
            return periodDesc[timePeriod - 1];
        }
        return "未知时段";
    }

    /**
     * 获取状态变更操作描述
     */
    private String getStatusActionDesc(String messageType) {
        if (AppointmentMessageDTO.MessageType.APPOINTMENT_CANCEL.name().equals(messageType)) {
            return "取消";
        } else if (AppointmentMessageDTO.MessageType.APPOINTMENT_MISS.name().equals(messageType)) {
            return "失约";
        }
        return "更新";
    }
}
