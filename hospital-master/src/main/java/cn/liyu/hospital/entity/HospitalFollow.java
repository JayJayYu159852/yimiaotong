package cn.liyu.hospital.entity;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * 医院关注实体
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Schema(description = "医院关注")
@Data
public class HospitalFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关注编号")
    private Long id;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "医院编号")
    private Long hospitalId;

    @Schema(description = "创建时间")
    private Date gmtCreate;
}
