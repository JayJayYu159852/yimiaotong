package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class HospitalDoctor implements Serializable {
    /**
     * 医生编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "医生编号")
    private Long id;
    /**
     * 医生姓名
     */
    @Schema(description = "医生姓名")
    private String name;
    /**
     * 性别：1，男；2，女
     */
    @Schema(description = "性别：1，男；2，女")
    private Integer gender;
    /**
     * 医生职称
     */
    @Schema(description = "医生职称")
    private String jobTitle;
    /**
     * 医生专长
     */
    @Schema(description = "医生专长")
    private String specialty;
    /**
     * 所属专科
     */
    @Schema(description = "所属专科")
    private Long specialId;
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
    public String getJobTitle() {
        return jobTitle;
    }
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle == null ? null : jobTitle.trim();
    }
    public String getSpecialty() {
        return specialty;
    }
    public void setSpecialty(String specialty) {
        this.specialty = specialty == null ? null : specialty.trim();
    }
    public Long getSpecialId() {
        return specialId;
    }
    public void setSpecialId(Long specialId) {
        this.specialId = specialId;
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
        sb.append(", jobTitle=").append(jobTitle);
        sb.append(", specialty=").append(specialty);
        sb.append(", specialId=").append(specialId);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
}
}
