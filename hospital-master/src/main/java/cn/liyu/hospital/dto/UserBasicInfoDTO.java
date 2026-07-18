package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理端用户信息 DTO（聚合 UserBasicInfo + PowerAccount.status）
 *
 * @author 医秒通
 */
@Schema(description = "管理端用户信息")
@Data
public class UserBasicInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户编号")
    private Long id;

    @Schema(description = "昵称/姓名")
    private String name;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "账号状态 1：正常，0：禁用")
    private Integer status;

    @Schema(description = "注册时间")
    private Date gmtCreate;
}
