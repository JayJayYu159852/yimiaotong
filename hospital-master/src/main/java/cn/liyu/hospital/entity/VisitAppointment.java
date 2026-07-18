package cn.liyu.hospital.entity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class VisitAppointment implements Serializable {
    /**
     * 预约编号
     */
    /**
     *
     * @mbg.generated
     */
    /**
     * 序列化为字符串：秒杀单编号为 RedisIdWorker 生成的 64 位大数，
     * 超出 JS 安全整数范围（2^53），返回数字会在前端精度丢失
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "预约编号")
    private Long id;
    /**
     * 出诊编号
     */
    @Schema(description = "出诊编号")
    private Long planId;
    /**
     * 就诊卡号
     */
    @Schema(description = "就诊卡号")
    private Long cardId;
    /**
     * 账号编号
     */
    @Schema(description = "账号编号")
    private Long accountId;
    /**
     * 1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5: 10点半~11点，6： 11点~11点半，7： 11点半~12点，8：2点~2点半，9： 2点半~3点，10： 3点~3点半，11： 3点半~4点，12： 4点~4点半，13： 4点半~5点，14： 5点~5点半，15：5点半~6点
     */
    @Schema(description = "1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5: 10点半~11点，6： 11点~11点半，7： 11点半~12点，8：2点~2点半，9： 2点半~3点，10： 3点~3点半，11： 3点半~4点，12： 4点~4点半，13： 4点半~5点，14： 5点~5点半，15：5点半~6点")
    private Integer timePeriod;
    /**
     * 预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成
     */
    @Schema(description = "预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成")
    private Integer status;
    /**
     * 支付状态：0=未支付, 1=已支付, 2=已退款
     */
    @Schema(description = "支付状态：0=未支付, 1=已支付, 2=已退款")
    private Integer payStatus;
    /**
     * 关联支付订单编号
     */
    @Schema(description = "关联支付订单编号")
    private Long paymentId;
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
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getPlanId() {
        return planId;
    }
    public void setPlanId(Long planId) {
        this.planId = planId;
    }
    public Long getCardId() {
        return cardId;
    }
    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }
    public Long getAccountId() {
        return accountId;
    }
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    public Integer getTimePeriod() {
        return timePeriod;
    }
    public void setTimePeriod(Integer timePeriod) {
        this.timePeriod = timePeriod;
    }
    public Integer getStatus() {
        return status;
    }
    public void setStatus(Integer status) {
        this.status = status;
    }
    public Integer getPayStatus() {
        return payStatus;
    }
    public void setPayStatus(Integer payStatus) {
        this.payStatus = payStatus;
    }
    public Long getPaymentId() {
        return paymentId;
    }
    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }
    public Date getGmtCreate() {
        return gmtCreate;
    }
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
    public Date getGmtModified() {
        return gmtModified;
    }
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", planId=").append(planId);
        sb.append(", cardId=").append(cardId);
        sb.append(", accountId=").append(accountId);
        sb.append(", timePeriod=").append(timePeriod);
        sb.append(", status=").append(status);
        sb.append(", payStatus=").append(payStatus);
        sb.append(", paymentId=").append(paymentId);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
}
}
