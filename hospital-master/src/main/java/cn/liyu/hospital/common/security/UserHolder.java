package cn.liyu.hospital.common.security;

import cn.liyu.hospital.dto.UserDTO;

/**
 * 用户上下文 — 基于 ThreadLocal，登录后将用户信息存入当前线程，
 * 方便业务代码通过 {@code UserHolder.getUser()} 直接获取。
 *
 * @author 医秒通
 */
public class UserHolder {

    private UserHolder() {}

    private static final ThreadLocal<UserDTO> TL = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        TL.set(user);
    }

    public static UserDTO getUser() {
        return TL.get();
    }

    public static void removeUser() {
        TL.remove();
    }
}
