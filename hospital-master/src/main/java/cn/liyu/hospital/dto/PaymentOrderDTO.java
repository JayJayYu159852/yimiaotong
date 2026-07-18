package cn.liyu.hospital.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付订单 DTO（含关联预约信息，提供给前端展示）
 * <p>
 * 注意：paymentId / appointmentId 使用 ToStringSerializer 序列化，
 * 防止 64位雪花ID 在 JavaScript 中丢失精度（JS Number 仅安全表示 2^53-1）。
 *
 * @author 医秒通
 */
@Data
public class PaymentOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long paymentId;

    private String paymentNo;
    private Long amount;
    private String amountYuan;
    private Integer status;
    private String statusDesc;
    private String payMethod;
    private Date payTime;
    private Date expireTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long appointmentId;

    private Date gmtCreate;
}
