package cn.liyu.hospital.component;

import cn.liyu.hospital.entity.PaymentOrder;
import cn.liyu.hospital.entity.PaymentWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * 模拟支付网关
 * <p>
 * 模拟微信支付 / 支付宝的统一下单、查询订单、退款等接口。
 * 在真实项目中，这一层会替换为微信支付 SDK（JSAPI/ Native / H5）或支付宝 SDK 的调用。
 * <p>
 * 当前实现：直接操作钱包余额，模拟支付网关的行为。
 * 调用方（PaymentOrderServiceImpl）通过此网关完成支付，不感知底层是"微信支付"还是"钱包余额"。
 * <p>
 * 设计模式：策略模式 —— 后续可扩展为 WechatPayGateway / AlipayGateway / WalletPayGateway。
 *
 * @author 医秒通
 */
@Component
public class SimulatedPaymentGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    /**
     * 模拟「统一下单」—— 对应微信支付 JSAPI 下单、支付宝 unifiedOrder
     * <p>
     * 真实场景：向微信/支付宝服务器发起 HTTPS 请求，返回 prepay_id / trade_no
     * 模拟场景：生成模拟的 prepayId，记录待支付订单
     *
     * @param order  支付订单
     * @param wallet 付款钱包
     * @return 支付平台返回的预支付信息（prepay_id, 签名等）
     */
    public UnifiedOrderResult unifiedOrder(PaymentOrder order, PaymentWallet wallet) {
        // 模拟生成 prepay_id（真实场景是微信/支付宝服务端返回的）
        String prepayId = "wx" + UUID.randomUUID().toString().replace("-", "").substring(0, 28);

        LOGGER.info("[模拟支付网关] 统一下单成功: paymentNo={}, prepayId={}, amount={}分",
                order.getPaymentNo(), prepayId, order.getAmount());

        UnifiedOrderResult result = new UnifiedOrderResult();
        result.setSuccess(true);
        result.setPrepayId(prepayId);
        result.setOutTradeNo(order.getPaymentNo());
        result.setTotalFee(order.getAmount());
        // 模拟返回给前端的支付参数（真实场景需要签名）
        result.setPaySign(generateMockSign(prepayId, order.getPaymentNo(), order.getAmount()));
        return result;
    }

    /**
     * 模拟「执行支付」—— 对应微信支付扣款 / 支付宝扣款
     * <p>
     * 真实场景：用户在微信/支付宝客户端确认支付后，支付平台回调通知我们
     * 模拟场景：直接从钱包扣款，模拟支付平台扣款成功
     *
     * @param order  支付订单
     * @param wallet 付款钱包
     * @return 支付结果
     */
    public PayResult executePay(PaymentOrder order, PaymentWallet wallet) {
        // 模拟网络延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 检查余额是否充足
        if (wallet.getBalance() < order.getAmount()) {
            PayResult result = new PayResult();
            result.setSuccess(false);
            result.setErrCode("ACCOUNT.BALANCE_NOT_ENOUGH");
            result.setErrMsg("余额不足，当前余额 " + wallet.getBalance() + " 分，需支付 " + order.getAmount() + " 分");
            LOGGER.warn("[模拟支付网关] 支付失败(余额不足): paymentNo={}", order.getPaymentNo());
            return result;
        }

        // 生成微信/支付宝交易号（真实场景由支付平台返回）
        String transactionId = "420000" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));

        PayResult result = new PayResult();
        result.setSuccess(true);
        result.setTransactionId(transactionId);
        result.setOutTradeNo(order.getPaymentNo());
        result.setTotalFee(order.getAmount());
        result.setPayTime(new Date());
        result.setPayMethod("WALLET");
        LOGGER.info("[模拟支付网关] 支付成功: paymentNo={}, transactionId={}", order.getPaymentNo(), transactionId);
        return result;
    }

    /**
     * 模拟「申请退款」—— 对应微信支付退款 / 支付宝退款
     * <p>
     * 真实场景：向微信/支付宝发起退款申请，异步处理
     * 模拟场景：同步返回退款成功
     *
     * @param order  原支付订单
     * @param reason 退款原因
     * @return 退款结果
     */
    public RefundResult applyRefund(PaymentOrder order, String reason) {
        // 模拟生成退款单号
        String refundId = "503" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));

        RefundResult result = new RefundResult();
        result.setSuccess(true);
        result.setRefundId(refundId);
        result.setOutTradeNo(order.getPaymentNo());
        result.setRefundFee(order.getAmount());
        result.setRefundTime(new Date());
        LOGGER.info("[模拟支付网关] 退款成功: paymentNo={}, refundId={}", order.getPaymentNo(), refundId);
        return result;
    }

    /**
     * 模拟「支付回调通知」—— 真实场景由支付平台异步 POST 到 notify_url
     * <p>
     * 当前项目简化：同步执行支付后直接处理结果，无需异步回调。
     * 但架构上保留此入口，方便后续扩展为真实的异步回调模式。
     *
     * @param transactionId 支付平台交易号
     * @param outTradeNo    商户订单号
     * @return 回调处理是否成功
     */
    public boolean handlePaymentNotify(String transactionId, String outTradeNo) {
        LOGGER.info("[模拟支付网关] 收到支付回调通知: transactionId={}, outTradeNo={}", transactionId, outTradeNo);
        // 真实场景需要：验签 → 查询订单 → 更新状态 → 返回应答
        return true;
    }

    /**
     * 生成模拟签名（真实场景使用 RSA/SM2 签名）
     */
    private String generateMockSign(String prepayId, String outTradeNo, Long totalFee) {
        return "MOCK_SIGN_" + prepayId.substring(0, 8);
    }

    // ==================== 支付网关返回对象 ====================

    /**
     * 统一下单返回结果
     */
    public static class UnifiedOrderResult {
        private boolean success;
        private String prepayId;
        private String outTradeNo;
        private Long totalFee;
        private String paySign;
        private String errMsg;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getPrepayId() { return prepayId; }
        public void setPrepayId(String prepayId) { this.prepayId = prepayId; }
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
        public Long getTotalFee() { return totalFee; }
        public void setTotalFee(Long totalFee) { this.totalFee = totalFee; }
        public String getPaySign() { return paySign; }
        public void setPaySign(String paySign) { this.paySign = paySign; }
        public String getErrMsg() { return errMsg; }
        public void setErrMsg(String errMsg) { this.errMsg = errMsg; }
    }

    /**
     * 支付执行结果
     */
    public static class PayResult {
        private boolean success;
        private String transactionId;
        private String outTradeNo;
        private Long totalFee;
        private Date payTime;
        private String payMethod;
        private String errCode;
        private String errMsg;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
        public Long getTotalFee() { return totalFee; }
        public void setTotalFee(Long totalFee) { this.totalFee = totalFee; }
        public Date getPayTime() { return payTime; }
        public void setPayTime(Date payTime) { this.payTime = payTime; }
        public String getPayMethod() { return payMethod; }
        public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
        public String getErrCode() { return errCode; }
        public void setErrCode(String errCode) { this.errCode = errCode; }
        public String getErrMsg() { return errMsg; }
        public void setErrMsg(String errMsg) { this.errMsg = errMsg; }
    }

    /**
     * 退款结果
     */
    public static class RefundResult {
        private boolean success;
        private String refundId;
        private String outTradeNo;
        private Long refundFee;
        private Date refundTime;
        private String errMsg;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getRefundId() { return refundId; }
        public void setRefundId(String refundId) { this.refundId = refundId; }
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }
        public Long getRefundFee() { return refundFee; }
        public void setRefundFee(Long refundFee) { this.refundFee = refundFee; }
        public Date getRefundTime() { return refundTime; }
        public void setRefundTime(Date refundTime) { this.refundTime = refundTime; }
        public String getErrMsg() { return errMsg; }
        public void setErrMsg(String errMsg) { this.errMsg = errMsg; }
    }
}
