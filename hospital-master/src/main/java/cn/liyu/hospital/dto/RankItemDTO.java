package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 排行数据项
 *
 * @author 医秒通
 */
@Schema(description = "排行数据项")
@Data
public class RankItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private Long value;

    @Schema(description = "占比（%）")
    private Double percentage;
}
