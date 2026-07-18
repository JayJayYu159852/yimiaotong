package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "账号注册对象参数")
@Data
public class PowerAccountRegisterParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 登录账号 唯一
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "登录账号 唯一", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 登录密码 使用md5加密
     */
    @Schema(description = "登录密码 使用md5加密", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
