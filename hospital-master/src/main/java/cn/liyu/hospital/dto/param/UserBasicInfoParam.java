package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
    /**
 * 普通用户信息参数
     */
    /**
 *
 * @author 医秒通
     */
    /**
    */
@Schema(description = "普通用户信息参数")
@Data
public class UserBasicInfoParam implements Serializable {
    /**
     * 姓名
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 用户头像
     */
    @Schema(description = "用户头像")
    private String avatarUrl;
}
