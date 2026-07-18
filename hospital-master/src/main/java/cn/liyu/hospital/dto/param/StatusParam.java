package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "状态参数")
@Data
public class StatusParam implements Serializable {
    @Schema(description = "状态 1：启用，0：禁用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
