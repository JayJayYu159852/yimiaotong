package cn.liyu.hospital.dto;

import cn.liyu.hospital.entity.UserCase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病例封装对象
 * <p>
 * 在病例记录基础上聚合就诊信息中文名称（医生/医院/专科/门诊），
 * 供用户端"我的病历"页面展示，避免直接展示编号。
 *
 * @author 医秒通
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "病例封装对象")
public class UserCaseDTO extends UserCase {

    private static final long serialVersionUID = 1L;

    @Schema(description = "医生名称")
    private String doctorName;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "专科名称")
    private String specialName;

}
