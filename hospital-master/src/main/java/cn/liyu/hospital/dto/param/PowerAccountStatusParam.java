package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * @author 医秒通
 */
@Schema(description = "账号状态修改参数")
@Data
public class PowerAccountStatusParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "账号编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long accountId;

    @Schema(description = "账号状态（1：开启，0：关闭）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
