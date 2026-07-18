package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class PaymentFlow implements Serializable {
    /**
     * 流水编号
     */
    @Schema(description = "流水编号")
    private Long id;
    /**
     * 支付订单编号
     */
    @Schema(description = "支付订单编号")
    private Long paymentId;
    /**
     * 支付单号
     */
    @Schema(description = "支付单号")
    private String paymentNo;
    /**
     * 账号编号
     */
    @Schema(description = "账号编号")
    private Long accountId;
    /**
     * 变动金额（分，正数=扣款）
     */
    @Schema(description = "变动金额（分）")
    private Long amount;
    /**
     * 变动前余额（分）
     */
    @Schema(description = "变动前余额（分）")
    private Long balanceBefore;
    /**
     * 变动后余额（分）
     */
    @Schema(description = "变动后余额（分）")
    private Long balanceAfter;
    /**
     * 流水类型：1=支付扣款, 2=退款入账, 3=过期退回
     */
    @Schema(description = "流水类型：1=支付扣款, 2=退款入账, 3=过期退回")
    private Integer flowType;
    /**
     * 流水状态：1=成功
     */
    @Schema(description = "流水状态：1=成功")
    private Integer flowStatus;
    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date gmtCreate;
    private static final long serialVersionUID = 1L;
}
