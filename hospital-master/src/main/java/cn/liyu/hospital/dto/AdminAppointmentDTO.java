package cn.liyu.hospital.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理端预约列表封装对象
 * <p>
 * 聚合预约记录 + 病人信息（就诊卡） + 出诊计划信息（医生/医院/日期），
 * 供管理端"预约列表"页面展示与操作（标记完成/失约）。
 *
 * @author 医秒通
 */
@Schema(description = "管理端预约列表封装对象")
@Data
public class AdminAppointmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 序列化为字符串：秒杀单编号为 64 位大数，超出 JS 安全整数范围（2^53），
     * 直接返回数字会在前端精度丢失，导致按 ID 操作时报"不存在该预约编号"
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "预约编号")
    private Long appointmentId;

    @Schema(description = "出诊计划编号")
    private Long planId;

    @Schema(description = "就诊卡编号")
    private Long cardId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "医生名称")
    private String doctorName;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "出诊日期")
    private Date day;

    @Schema(description = "时间段：1 上午，2 下午")
    private Integer time;

    @Schema(description = "就诊时间段：1~7 上午，8~15 下午")
    private Integer timePeriod;

    @Schema(description = "预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成")
    private Integer status;

    @Schema(description = "支付状态 0：未支付，1：已支付，2：已退款")
    private Integer payStatus;

    @Schema(description = "创建时间")
    private Date gmtCreate;
}
