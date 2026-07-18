package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "用户病例参数")
@Data
public class UserCaseParam implements Serializable {
    /**
     * 就诊卡编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "就诊卡编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cardId;
    /**
     * 预约编号
     */
    @Schema(description = "预约编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long appointmentId;
    /**
     * 医生编号
     */
    @Schema(description = "医生编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long doctorId;
    /**
     * 病例详情
     */
    @Schema(description = "病例详情", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
