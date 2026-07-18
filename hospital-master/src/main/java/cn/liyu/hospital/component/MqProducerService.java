package cn.liyu.hospital.component;

import cn.liyu.hospital.config.RabbitMQConfig;
import cn.liyu.hospital.dto.AppointmentMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * 消息队列生产者服务（需要 RabbitMQ Profile 激活）
 * <p>
 * 负责将预约相关消息投递到 RabbitMQ，包含三个核心场景：
 * 1. sendAppointmentNotify() — 预约成功，立即通知
 * 2. sendAppointmentReminder() — 就诊提醒，延迟投递（通过 DLX 实现）
 * 3. sendStatusChange() — 状态变更，Fanout 广播通知
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Profile("rabbitmq")
@Component
public class MqProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqProducerService.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送预约成功即时通知
     * <p>
     * 消息直接投递到 appointmentExchange → appointmentNotifyQueue，
     * 消费者立即收到并发送短信通知患者。
     *
     * @param message 预约消息体
     */
    public void sendAppointmentNotify(AppointmentMessageDTO message) {
        message.setMessageType(AppointmentMessageDTO.MessageType.APPOINTMENT_SUCCESS.name());
        message.setCreateTime(new Date());

        CorrelationData correlationData = new CorrelationData(
                UUID.randomUUID().toString().replace("-", "")
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.APPOINTMENT_EXCHANGE,
                RabbitMQConfig.APPOINTMENT_NOTIFY_ROUTING_KEY,
                message,
                correlationData
        );

        LOGGER.info("预约成功通知已发送 -> 预约ID: {}, 患者: {}, 医生: {}",
                message.getAppointmentId(), message.getPatientName(), message.getDoctorName());
    }

    /**
     * 发送就诊提醒（延迟消息）
     * <p>
     * 消息先投递到 delayQueue（TTL过期队列），经过指定延迟时间后，
     * 自动转入 reminderExchange → reminderQueue，由消费者处理。
     * 典型用法：就诊前一天发送提醒短信。
     *
     * @param message 预约消息体
     * @param delayMs 延迟毫秒数（如 86400000 = 24小时）
     */
    public void sendAppointmentReminder(AppointmentMessageDTO message, long delayMs) {
        message.setMessageType(AppointmentMessageDTO.MessageType.APPOINTMENT_REMINDER.name());
        message.setCreateTime(new Date());

        CorrelationData correlationData = new CorrelationData(
                UUID.randomUUID().toString().replace("-", "")
        );

        // 使用 postProcessMessage 在消息级别设置 TTL（消息过期后自动进入死信交换机）
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.DELAY_ROUTING_KEY,
                message,
                msg -> {
                    // 设置消息级别的 TTL（过期后自动进入死信交换机）
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    return msg;
                },
                correlationData
        );

        LOGGER.info("就诊提醒延迟消息已发送 -> 预约ID: {}, 延迟: {}ms (约{}小时), 就诊日期: {}",
                message.getAppointmentId(), delayMs, delayMs / 3600000, message.getVisitDate());
    }

    /**
     * 发送预约状态变更通知（Fanout 广播）
     * <p>
     * 消息投递到 statusExchange（Fanout），所有绑定的队列（SMS、邮件等）
     * 都会收到通知，实现多渠道同时推送。
     *
     * @param message 预约消息体
     */
    public void sendStatusChange(AppointmentMessageDTO message) {
        message.setCreateTime(new Date());

        CorrelationData correlationData = new CorrelationData(
                UUID.randomUUID().toString().replace("-", "")
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.STATUS_EXCHANGE,
                // Fanout 交换机忽略 routing key
                "",
                message,
                correlationData
        );

        String action = "";
        if (AppointmentMessageDTO.MessageType.APPOINTMENT_CANCEL.name().equals(message.getMessageType())) {
            action = "取消预约";
        } else if (AppointmentMessageDTO.MessageType.APPOINTMENT_MISS.name().equals(message.getMessageType())) {
            action = "失约";
        }

        LOGGER.info("状态变更通知(广播)已发送 -> {} -> 预约ID: {}, 患者: {}",
                action, message.getAppointmentId(), message.getPatientName());
    }

}
