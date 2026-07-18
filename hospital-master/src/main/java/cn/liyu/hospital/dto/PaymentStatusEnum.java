package cn.liyu.hospital.dto;

import lombok.Getter;

/**
 * 支付订单状态枚举
 *
 * @author 医秒通
 */
@Getter
public enum PaymentStatusEnum {
    UNPAID(0, "待支付"),
    PAYING(1, "支付中"),
    PAID(2, "支付成功"),
    FAILED(3, "支付失败"),
    REFUNDED(4, "已退款"),
    EXPIRED(5, "已过期");

    private final Integer status;
    private final String desc;

    PaymentStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static String getDescByStatus(Integer status) {
        for (PaymentStatusEnum e : values()) {
            if (e.getStatus().equals(status)) {
                return e.getDesc();
            }
        }
        return "未知";
    }
}
