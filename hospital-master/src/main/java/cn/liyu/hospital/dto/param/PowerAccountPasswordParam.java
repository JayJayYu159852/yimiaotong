package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "账号修改密码对象参数")
@Data
public class PowerAccountPasswordParam extends PowerAccountRegisterParam implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "短信验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
