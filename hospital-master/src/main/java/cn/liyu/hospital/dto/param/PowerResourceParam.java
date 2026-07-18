package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "权限资源参数")
@Data
public class PowerResourceParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 资源分类编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "资源分类编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;
    /**
     * 资源名称
     */
    @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 资源URL
     */
    @Schema(description = "资源URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;
    /**
     * 资源描述
     */
    @Schema(description = "资源描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}
