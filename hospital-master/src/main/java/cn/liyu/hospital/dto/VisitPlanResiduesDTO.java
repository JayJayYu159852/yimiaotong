package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.VisitPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.List;

/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "出诊计划含余额数")
@Data
public class VisitPlanResiduesDTO extends VisitPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "对应时间段，剩余号数")
    private List<Integer> residues;
}
