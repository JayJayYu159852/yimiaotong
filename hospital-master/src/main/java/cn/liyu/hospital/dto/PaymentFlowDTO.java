package cn.liyu.hospital.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付流水 DTO（交易明细）
 *
 * @author 医秒通
 */
@Data
public class PaymentFlowDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long paymentId;
    private String paymentNo;
    private Long amount;
    private Long balanceBefore;
    private Long balanceAfter;
    private Integer flowType;
    private String flowTypeDesc;
    private String remark;
    private Date gmtCreate;
}
