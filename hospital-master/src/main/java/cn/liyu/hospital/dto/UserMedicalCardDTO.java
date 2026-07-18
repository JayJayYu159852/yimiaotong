package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
/**
    /**
 * 用户医疗卡信息
     */
    /**
 *
 * @author 医秒通
     */
    /**
    */
@Schema(description = "用户医疗卡信息")
@Data
public class UserMedicalCardDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 关系编号
     */
    @Schema(description = "关系编号")
    private Long relationId;
    /**
     * 就诊卡号
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
}
