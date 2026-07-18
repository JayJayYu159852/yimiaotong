package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;
/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "挂号记录")
public class VisitAppointmentWithQueueDTO extends VisitAppointmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "就诊号")
    private Integer queueNum;
    @Schema(description = "创建时间")
    private Date gmtCreate;
}
