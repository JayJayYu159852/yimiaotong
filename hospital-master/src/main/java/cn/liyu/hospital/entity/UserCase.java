package cn.liyu.hospital.entity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class UserCase implements Serializable {
    /**
     * 记录编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "记录编号")
    private Long id;
    /**
     * 就诊卡编号
     */
    @Schema(description = "就诊卡编号")
    private Long cardId;
    /**
     * 预约编号
     */
    /**
     * 序列化为字符串：秒杀单编号为 64 位大数，超出 JS 安全整数范围，返回数字会在前端精度丢失
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "预约编号")
    private Long appointmentId;
    /**
     * 医生编号
     */
    @Schema(description = "医生编号")
    private Long doctorId;
    /**
     * 病例详情
     */
    @Schema(description = "病例详情")
    private String content;
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
    public Long getCardId() {
        return cardId;
    }
    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }
    public Long getAppointmentId() {
        return appointmentId;
    }
    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }
    public Long getDoctorId() {
        return doctorId;
    }
    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content == null ? null : content.trim();
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
        sb.append(", cardId=").append(cardId);
        sb.append(", appointmentId=").append(appointmentId);
        sb.append(", doctorId=").append(doctorId);
        sb.append(", content=").append(content);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
}
}
