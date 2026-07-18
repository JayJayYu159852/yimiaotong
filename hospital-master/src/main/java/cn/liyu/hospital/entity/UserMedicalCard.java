package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class UserMedicalCard implements Serializable {
    /**
     * 就诊卡号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "就诊卡号")
    private Long id;
    /**
     * 姓名
     */
    @Schema(description = "姓名")
    private String name;
    /**
     * 性别 男：1，女：2
     */
    @Schema(description = "性别 男：1，女：2")
    private Integer gender;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 证件号
     */
    @Schema(description = "证件号")
    private String identificationNumber;
    /**
     * 出生日期
     */
    @Schema(description = "出生日期")
    private Date birthDate;
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
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
    public Integer getGender() {
        return gender;
    }
    public void setGender(Integer gender) {
        this.gender = gender;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }
    public String getIdentificationNumber() {
        return identificationNumber;
    }
    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber == null ? null : identificationNumber.trim();
    }
    public Date getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
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
        sb.append(", name=").append(name);
        sb.append(", gender=").append(gender);
        sb.append(", phone=").append(phone);
        sb.append(", identificationNumber=").append(identificationNumber);
        sb.append(", birthDate=").append(birthDate);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
}
}
