package cn.liyu.hospital.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息队列配置类
 * <p>
 * 包含三个核心场景：
 * 1. 预约成功即时通知 — Direct Exchange + 普通队列
 * 2. 就诊提醒延迟通知 — 通过死信队列（DLX）实现延迟消息
 * 3. 预约状态变更通知 — Fanout Exchange 广播模式
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Configuration
@Profile("rabbitmq")
public class RabbitMQConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConfig.class);

    // ==================== 场景1：预约成功即时通知 ====================

    public static final String APPOINTMENT_EXCHANGE = "hospital.appointment.exchange";
    public static final String APPOINTMENT_NOTIFY_QUEUE = "hospital.appointment.notify.queue";
    public static final String APPOINTMENT_NOTIFY_ROUTING_KEY = "appointment.notify";

    /**
     * 预约直属交换机（Direct）
     */
    @Bean
    public DirectExchange appointmentExchange() {
        return ExchangeBuilder
                .directExchange(APPOINTMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 预约通知队列
     */
    @Bean
    public Queue appointmentNotifyQueue() {
        return QueueBuilder
                .durable(APPOINTMENT_NOTIFY_QUEUE)
                .build();
    }

    /**
     * 绑定通知队列到交换机
     */
    @Bean
    public Binding appointmentNotifyBinding() {
        return BindingBuilder
                .bind(appointmentNotifyQueue())
                .to(appointmentExchange())
                .with(APPOINTMENT_NOTIFY_ROUTING_KEY);
    }

    // ==================== 场景2：就诊提醒 — 延迟队列（DLX实现） ====================

    public static final String DELAY_EXCHANGE = "hospital.appointment.delay.exchange";
    public static final String DELAY_QUEUE = "hospital.appointment.delay.queue";
    public static final String DELAY_ROUTING_KEY = "appointment.delay";

    public static final String REMINDER_EXCHANGE = "hospital.appointment.reminder.exchange";
    public static final String REMINDER_QUEUE = "hospital.appointment.reminder.queue";
    public static final String REMINDER_ROUTING_KEY = "appointment.reminder";

    /**
     * 延迟交换机（Dead Letter Exchange 死信交换机）
     * 消息在延迟队列过期后，会被自动转发到 reminderExchange
     */
    @Bean
    public DirectExchange reminderExchange() {
        return ExchangeBuilder
                .directExchange(REMINDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 就诊提醒队列（实际消费队列）
     */
    @Bean
    public Queue reminderQueue() {
        return QueueBuilder
                .durable(REMINDER_QUEUE)
                .build();
    }

    @Bean
    public Binding reminderBinding() {
        return BindingBuilder
                .bind(reminderQueue())
                .to(reminderExchange())
                .with(REMINDER_ROUTING_KEY);
    }

    /**
     * 延迟转发交换机
     */
    @Bean
    public DirectExchange delayExchange() {
        return ExchangeBuilder
                .directExchange(DELAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 延迟队列（设置 DLX，消息过期后自动转发到提醒交换机）
     * <p>
     * 不设置队列级别的 x-message-ttl，由发送方在消息级别指定 TTL，
     * 避免队列 TTL 截断较长的延迟消息（RabbitMQ 取 min(队列TTL, 消息TTL)）。
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 声明死信交换机（DLX）
        args.put("x-dead-letter-exchange", REMINDER_EXCHANGE);
        // 声明死信路由键
        args.put("x-dead-letter-routing-key", REMINDER_ROUTING_KEY);

        return QueueBuilder
                .durable(DELAY_QUEUE)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder
                .bind(delayQueue())
                .to(delayExchange())
                .with(DELAY_ROUTING_KEY);
    }

    // ==================== 场景3：状态变更通知（Fanout 广播） ====================

    public static final String STATUS_EXCHANGE = "hospital.appointment.status.fanout.exchange";
    public static final String STATUS_SMS_QUEUE = "hospital.appointment.status.sms.queue";

    /**
     * 状态变更广播交换机（Fanout — 无视 routing key，所有绑定的队列都收到消息）
     */
    @Bean
    public FanoutExchange statusExchange() {
        return ExchangeBuilder
                .fanoutExchange(STATUS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 状态变更通知队列（SMS 渠道）
     */
    @Bean
    public Queue statusSmsQueue() {
        return QueueBuilder
                .durable(STATUS_SMS_QUEUE)
                .build();
    }

    @Bean
    public Binding statusSmsBinding() {
        return BindingBuilder
                .bind(statusSmsQueue())
                .to(statusExchange());
    }

    // ==================== 通用配置 ====================

    /**
     * JSON 消息转换器（替代默认的 SimpleMessageConverter）
     * 避免 JDK 序列化，消息体可在 RabbitMQ 管理界面直接查看
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate（JSON 序列化 + 发布确认 + 回退回调）
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);

        // 发布确认回调（消息是否到达 Exchange）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && correlationData != null) {
                LOGGER.error("消息发送到 Exchange 失败: correlationId={}, 原因: {}", correlationData.getId(), cause);
            }
        });

        // 退回回调（消息未路由到 Queue）
        rabbitTemplate.setReturnsCallback(returned -> {
            LOGGER.error("消息未能路由到队列: body={}, replyCode={}",
                    new String(returned.getMessage().getBody()), returned.getReplyCode());
        });

        return rabbitTemplate;
    }
}
