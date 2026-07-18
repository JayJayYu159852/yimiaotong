package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 附近医院 DTO（GEO搜索结果）
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Schema(description = "附近医院信息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalNearbyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "医院编号")
    private Long hospitalId;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "医院地址")
    private String address;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "距离（公里）")
    private Double distance;
}
