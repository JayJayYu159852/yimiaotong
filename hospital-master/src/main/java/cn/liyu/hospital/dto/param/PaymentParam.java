package cn.liyu.hospital.dto.param;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付请求参数
 *
 * @author 医秒通
 */
@Data
public class PaymentParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long accountId;
    /** 支付密码（模拟环境不校验） */
    private String payPassword;
}
