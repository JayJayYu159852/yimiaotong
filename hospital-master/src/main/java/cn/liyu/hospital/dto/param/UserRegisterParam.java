package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * 普通用户基础信息参数
 *
 * @author 医秒通
 */
@Schema(description = "普通用户注册参数")
@Data
public class UserRegisterParam implements Serializable {

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "手机号（与接收验证码的号码一致，不可修改）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Schema(description = "短信验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "登录密码 使用md5加密", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
