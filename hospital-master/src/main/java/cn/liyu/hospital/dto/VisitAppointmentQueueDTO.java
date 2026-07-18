package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "候诊队列")
@Data
public class VisitAppointmentQueueDTO extends VisitAppointmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "前方候诊人数")
    private Integer waitPeopleNum;

    @Schema(description = "就诊号")
    private Integer queueNum;

    @Schema(description = "还需等待时间长")
    private Integer waitTime;

    @Schema(description = "预约时间段")
    private Integer timePeriod;
}
