package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.VisitAppointment;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
/**
 * @author 医秒通
 */
@Schema(description = "预约记录情况")
@Data
public class VisitAppointmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "预约记录编号")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long appointmentId;
    @Schema(description = "医院名称")
    private String hospitalName;
    @Schema(description = "专科名称")
    private String specialName;
    @Schema(description = "医生名称")
    private String doctorName;
    @Schema(description = "患者名称")
    private String name;
    @Schema(description = "时间段 1：上午，2：下午")
    private Integer time;
    @Schema(description = "出诊日期")
    private Date day;
    @Schema(description = "支付状态 0：未支付，1：已支付，2：已退款")
    private Integer payStatus;
    @Schema(description = "预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成")
    private Integer status;
}
