package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "权限账号修改密码参数")
@Data
public class PowerAccountUpdatePasswordParam implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "账号编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long accountId;
    @Schema(description = "旧密码 必须加密", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
    @Schema(description = "新密码 必须加密", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
