package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "权限角色参数")
@Data
public class PowerRoleParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 英文名称
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "英文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 中文名称
     */
    @Schema(description = "中文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chineseName;
    /**
     * 角色状态 1：启用，0：禁用
     */
    @Schema(description = "角色状态 1：启用，0：禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
