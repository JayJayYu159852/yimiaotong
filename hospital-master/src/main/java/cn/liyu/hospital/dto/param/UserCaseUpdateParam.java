package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "用户病例更新参数")
@Data
public class UserCaseUpdateParam implements Serializable {
    /**
     * 病例详情
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "病例详情", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
