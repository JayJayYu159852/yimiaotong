package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class HospitalSpecialRelation implements Serializable {
    /**
     * 关系编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "关系编号")
    private Long id;
    /**
     * 医院编号
     */
    @Schema(description = "医院编号")
    private Long hospitalId;
    /**
     * 专科编号
     */
    @Schema(description = "专科编号")
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
    public Long getHospitalId() {
        return hospitalId;
    }
    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
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
        sb.append(", hospitalId=").append(hospitalId);
        sb.append(", specialId=").append(specialId);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
}
}
