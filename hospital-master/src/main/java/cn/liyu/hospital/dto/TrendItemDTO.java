package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 趋势数据点
 *
 * @author 医秒通
 */
@Schema(description = "趋势数据点")
@Data
public class TrendItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期（yyyy-MM-dd）")
    private String date;

    @Schema(description = "数值")
    private Long count;
}
