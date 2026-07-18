package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class PaymentOrder implements Serializable {
    /**
     * 支付订单编号（RedisIdWorker全局唯一ID）
     */
    @Schema(description = "支付订单编号")
    private Long id;
    /**
     * 支付单号（PAY+时间戳+随机数，展示给用户）
     */
    @Schema(description = "支付单号")
    private String paymentNo;
    /**
     * 付款账号编号
     */
    @Schema(description = "付款账号编号")
    private Long accountId;
    /**
     * 关联预约编号
     */
    @Schema(description = "关联预约编号")
    private Long appointmentId;
    /**
     * 支付金额（单位：分）
     */
    @Schema(description = "支付金额（分）")
    private Long amount;
    /**
     * 支付状态：0=待支付, 1=支付中, 2=支付成功, 3=支付失败, 4=已退款, 5=已过期
     */
    @Schema(description = "支付状态：0=待支付, 1=支付中, 2=支付成功, 3=支付失败, 4=已退款, 5=已过期")
    private Integer status;
    /**
     * 支付方式：WALLET=钱包余额
     */
    @Schema(description = "支付方式")
    private String payMethod;
    /**
     * 支付完成时间
     */
    @Schema(description = "支付完成时间")
    private Date payTime;
    /**
     * 支付过期时间
     */
    @Schema(description = "支付过期时间")
    private Date expireTime;
    /**
     * 退款金额（分）
     */
    @Schema(description = "退款金额（分）")
    private Long refundAmount;
    /**
     * 退款时间
     */
    @Schema(description = "退款时间")
    private Date refundTime;
    /**
     * 退款原因
     */
    @Schema(description = "退款原因")
    private String refundReason;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date gmtCreate;
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date gmtModified;
    private static final long serialVersionUID = 1L;
}
