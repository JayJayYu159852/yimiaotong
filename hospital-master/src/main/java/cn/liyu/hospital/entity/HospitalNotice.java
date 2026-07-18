package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * 医院健康资讯实体
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Schema(description = "医院健康资讯")
@Data
public class HospitalNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资讯编号")
    private Long id;

    @Schema(description = "医院编号")
    private Long hospitalId;

    @Schema(description = "资讯标题")
    private String title;

    @Schema(description = "资讯内容")
    private String content;

    @Schema(description = "配图URL")
    private String picture;

    @Schema(description = "医院名称（关联查询）")
    private String hospitalName;

    @Schema(description = "创建时间")
    private Date gmtCreate;

    @Schema(description = "更新时间")
    private Date gmtModified;
}
