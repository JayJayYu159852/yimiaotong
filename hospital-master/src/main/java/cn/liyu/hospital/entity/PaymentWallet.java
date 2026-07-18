package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
@Data
public class PaymentWallet implements Serializable {
    /**
     * 钱包编号
     */
    @Schema(description = "钱包编号")
    private Long id;
    /**
     * 账号编号
     */
    @Schema(description = "账号编号")
    private Long accountId;
    /**
     * 余额（单位：分）
     */
    @Schema(description = "余额（单位：分）")
    private Long balance;
    /**
     * 乐观锁版本号
     */
    @Schema(description = "乐观锁版本号")
    private Integer version;
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
    private static final long serialVersionUID = 1L;
}
