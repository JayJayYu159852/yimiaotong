package cn.liyu.hospital.dto;

import lombok.Getter;

/**
 * 预约支付状态枚举（visit_appointment.pay_status）
 *
 * @author 医秒通
 */
@Getter
public enum PayStatusEnum {
    NOT_PAID(0, "未支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款");

    private final Integer status;
    private final String desc;

    PayStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static String getDescByStatus(Integer status) {
        for (PayStatusEnum e : values()) {
            if (e.getStatus().equals(status)) {
                return e.getDesc();
            }
        }
        return "未知";
    }
}
