package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "医院专科关系参数")
@Data
public class HospitalSpecialRelationParam implements Serializable {
    private static final long serialVersionUID = 1L;
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
}
