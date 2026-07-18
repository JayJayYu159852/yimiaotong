package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "医院信息参数")
@Data
public class HospitalInfoParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 医院名称
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "医院名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    /**
     * 医院电话
     */
    @Schema(description = "医院电话", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
    /**
     * 医院地址
     */
    @Schema(description = "医院地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;
    /**
     * 医院简介
     */
    @Schema(description = "医院简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
    /**
     * 医院图片
     */
    @Schema(description = "医院图片")
    private String picture;

    @Schema(description = "纬度（用于附近医院搜索）")
    private Double latitude;

    @Schema(description = "经度（用于附近医院搜索）")
    private Double longitude;
}
