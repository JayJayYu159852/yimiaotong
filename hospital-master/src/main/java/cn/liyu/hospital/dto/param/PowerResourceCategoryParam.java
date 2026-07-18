package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "权限资源分类参数")
@Data
public class PowerResourceCategoryParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 分类名称
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 分类排序 数值越小，越靠前
     */
    @Schema(description = "分类排序 数值越小，越靠前", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;
}
