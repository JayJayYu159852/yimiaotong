package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.HospitalInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
/**
 * @author 医秒通
 */
@Data
@Schema(description = "医生出诊信息列表")
public class VisitPlanListDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "医院信息")
    private HospitalInfo info;
    @Schema(description = "整合剩余挂号数的出诊计划列表")
    private List<VisitPlanResiduesDTO> planResiduesDTOList;
}
