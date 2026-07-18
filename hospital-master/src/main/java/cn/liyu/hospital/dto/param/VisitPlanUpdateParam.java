package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;
/**
 * @author 医秒通
 */
@Schema(description = "出诊计划参数")
@Data
public class VisitPlanUpdateParam {
    /**
     * 医院编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "医院编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long hospitalId;
    /**
     * 专科编号
     */
    @Schema(description = "专科编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long specialId;
    /**
     * 医生编号
     */
    @Schema(description = "医生编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long doctorId;
    /**
     * 时间段 1：上午，2：下午
     */
    @Schema(description = "时间段 1：上午，2：下午", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer time;
    /**
     * 出诊日期
     */
    @Schema(description = "出诊日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date day;
}
