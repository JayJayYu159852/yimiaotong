package cn.liyu.hospital.dto;

import lombok.Data;

/**
 * 用户信息 DTO（存入 ThreadLocal，供业务代码直接取用）
 *
 * @author 医秒通
 */
@Data
public class UserDTO {

    /** 账号编号 */
    private Long id;

    /** 手机号（即登录账号名） */
    private String phone;

    /** 用户姓名 */
    private String name;

    /** 头像 URL */
    private String avatarUrl;

    public static UserDTO empty() {
        return new UserDTO();
    }
}
