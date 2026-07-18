package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 预约挂号消息队列 DTO
 * <p>
 * 用于 RabbitMQ 消息传递，支持：
 * 1. 预约成功即时通知
 * 2. 就诊前延迟提醒（通过 DLX 死信队列实现）
 * 3. 预约状态变更通知
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Schema(description = "预约挂号MQ消息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型：
     * APPOINTMENT_SUCCESS — 预约成功通知
     * APPOINTMENT_REMINDER — 就诊提醒（延迟消息）
     * APPOINTMENT_CANCEL   — 预约取消通知
     * APPOINTMENT_MISS     — 失约通知
     */
    @Schema(description = "消息类型")
    private String messageType;

    /**
     * 预约编号
     */
    @Schema(description = "预约编号")
    private Long appointmentId;

    /**
     * 出诊计划编号
     */
    @Schema(description = "出诊计划编号")
    private Long planId;

    /**
     * 就诊卡号
     */
    @Schema(description = "就诊卡号")
    private Long cardId;

    /**
     * 账号编号
     */
    @Schema(description = "账号编号")
    private Long accountId;

    /**
     * 患者姓名
     */
    @Schema(description = "患者姓名")
    private String patientName;

    /**
     * 手机号码（用于发送短信通知）
     */
    @Schema(description = "手机号码")
    private String phoneNumber;

    /**
     * 医生姓名
     */
    @Schema(description = "医生姓名")
    private String doctorName;

    /**
     * 就诊日期
     */
    @Schema(description = "就诊日期")
    private Date visitDate;

    /**
     * 时间段（1:8:30-9:00 ~ 14:17:30-18:00）
     */
    @Schema(description = "时间段")
    private Integer timePeriod;

    /**
     * 消息创建时间
     */
    @Schema(description = "消息创建时间")
    private Date createTime;

    public enum MessageType {
        APPOINTMENT_SUCCESS,
        APPOINTMENT_REMINDER,
        APPOINTMENT_CANCEL,
        APPOINTMENT_MISS
    }
}
