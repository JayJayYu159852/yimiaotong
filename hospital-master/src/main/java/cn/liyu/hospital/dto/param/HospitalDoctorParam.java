package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "医生信息参数")
@Data
public class HospitalDoctorParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 医生姓名
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "医生姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 性别：1，男；2，女
     */
    @Schema(description = "性别：1，男；2，女", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gender;
    /**
     * 医生职称
     */
    @Schema(description = "医生职称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobTitle;
    /**
     * 医生专长
     */
    @Schema(description = "医生专长", requiredMode = Schema.RequiredMode.REQUIRED)
    private String specialty;
    /**
     * 专科编号
     */
    @Schema(description = "专科编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long specialId;
}
