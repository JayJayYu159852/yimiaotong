package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
/**
 * @author 医秒通
 */
@Schema(description = "用户医疗卡信息更新参数")
@Data
public class UserMedicalCardUpdateParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 姓名
     */
    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 手机号
     */
    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
    /**
     * 性别 男：1，女：2
     */
    @Schema(description = "性别 男：1，女：2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gender;
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
}
