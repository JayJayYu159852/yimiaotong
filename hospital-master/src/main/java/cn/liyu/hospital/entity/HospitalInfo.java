package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;

@Data
public class HospitalInfo implements Serializable {
    @Schema(description = "医院编号 从1001开始")
    private Long id;

    @Schema(description = "医院名称")
    private String name;

    @Schema(description = "医院电话")
    private String phone;

    @Schema(description = "医院地址")
    private String address;

    @Schema(description = "医院简介")
    private String description;

    @Schema(description = "医院图片")
    private String picture;

    @Schema(description = "纬度（用于GEO附近搜索）")
    private Double latitude;

    @Schema(description = "经度（用于GEO附近搜索）")
    private Double longitude;

    @Schema(description = "创建时间")
    private Date gmtCreate;

    @Schema(description = "更新时间")
    private Date gmtModified;

    private static final long serialVersionUID = 1L;
}
