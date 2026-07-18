package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.UserCase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "就诊记录详情")
@Data
public class VisitAppointmentWithCaseDTO extends VisitAppointmentDTO {
    private static final long serialVersionUID = 1L;
    @Schema(description = "病例详情")
    private UserCase userCase;
}
