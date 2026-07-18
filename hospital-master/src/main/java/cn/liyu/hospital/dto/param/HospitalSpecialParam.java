package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "医院专科参数")
@Data
public class HospitalSpecialParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 专科名称
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "专科名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 专科简介
     */
    @Schema(description = "专科简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}
