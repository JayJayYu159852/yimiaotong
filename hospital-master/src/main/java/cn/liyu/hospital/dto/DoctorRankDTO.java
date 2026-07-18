package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 医生排行榜 DTO
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Schema(description = "医生好评排行榜")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRankDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "医生编号")
    private Long doctorId;

    @Schema(description = "医生姓名")
    private String doctorName;

    @Schema(description = "职称")
    private String jobTitle;

    @Schema(description = "所属科室")
    private String specialName;

    @Schema(description = "综合评分")
    private Double score;

    @Schema(description = "评价人数")
    private Long rateCount;
}
