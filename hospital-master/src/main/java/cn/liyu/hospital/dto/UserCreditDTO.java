package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "用户当月信用封装对象")
@Data
public class UserCreditDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "取消")
    private Integer cancel;
    @Schema(description = "完成")
    private Integer finish;
    @Schema(description = "失约")
    private Integer miss;
}
