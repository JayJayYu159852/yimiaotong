package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.VisitPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "出诊计划封装对象")
public class VisitPlanDTO extends VisitPlan {
    private static final long serialVersionUID = 1L;

    @Schema(description = "医生名称")
    private String doctorName;

    @Schema(description = "专科名称")
    private String specialName;

    @Schema(description = "医院名称")
    private String hospitalName;
}
