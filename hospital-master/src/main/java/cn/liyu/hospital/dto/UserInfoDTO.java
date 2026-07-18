package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.PowerAccount;
import cn.liyu.hospital.entity.UserBasicInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "用户信息封装对象")
@Data
public class UserInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "账号信息")
    private PowerAccount account;
    @Schema(description = "用户信息")
    private UserBasicInfo basicInfo;
}
