package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户端登录参数 — 手机号必填，验证码和密码二选一
 *
 * @author 医秒通
 */
@Schema(description = "用户登录参数")
@Data
public class UserLoginParam {

    @Schema(description = "手机号（必填）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "短信验证码（与密码二选一）")
    private String code;

    @Schema(description = "登录密码（与验证码二选一）")
    private String password;
}
