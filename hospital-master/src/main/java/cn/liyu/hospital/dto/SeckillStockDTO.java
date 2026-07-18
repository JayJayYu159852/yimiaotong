package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀号源库存 DTO
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Schema(description = "秒杀号源库存")
@Data
public class SeckillStockDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "出诊计划编号")
    private Long planId;

    @Schema(description = "剩余号源数")
    private Long stock;

    @Schema(description = "是否已约满")
    private Boolean soldOut;
}
