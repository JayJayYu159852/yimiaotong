package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 营收趋势数据点
 *
 * @author 医秒通
 */
@Schema(description = "营收趋势数据点")
@Data
public class RevenueTrendItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期（yyyy-MM-dd）")
    private String date;

    @Schema(description = "营收金额（分）")
    private Long amount;

    @Schema(description = "营收金额（元）")
    private String amountYuan;
}
