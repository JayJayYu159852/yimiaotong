package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.HospitalDoctor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "医生信息封装类")
@Data
public class HospitalDoctorDTO extends HospitalDoctor implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "专科名称")
    private String specialName;
}
