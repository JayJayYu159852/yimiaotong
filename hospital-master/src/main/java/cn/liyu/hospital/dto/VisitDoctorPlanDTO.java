package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
/**
 * @author 医秒通
 */
@Schema(description = "医生号源情况")
@Data
public class VisitDoctorPlanDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "医生信息")
    private HospitalDoctorDTO doctorDTO;
    @Schema(description = "医生出诊信息列表")
    private List<VisitPlanListDTO> planListDTOList;
}
