package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘概览数据
 *
 * @author 医秒通
 */
@Schema(description = "仪表盘概览数据")
@Data
public class DashboardOverviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "今日挂号量")
    private Long todayAppointments;

    @Schema(description = "今日营收（分）")
    private Long todayRevenue;

    @Schema(description = "今日营收（元）")
    private String todayRevenueYuan;

    @Schema(description = "今日新增用户")
    private Long todayNewUsers;

    @Schema(description = "今日新增就诊卡")
    private Long todayNewCards;

    @Schema(description = "累计挂号量")
    private Long totalAppointments;

    @Schema(description = "累计营收（分）")
    private Long totalRevenue;

    @Schema(description = "累计营收（元）")
    private String totalRevenueYuan;

    @Schema(description = "累计用户数")
    private Long totalUsers;

    @Schema(description = "医院总数")
    private Long totalHospitals;

    @Schema(description = "医生总数")
    private Long totalDoctors;

    @Schema(description = "出诊计划总数")
    private Long totalPlans;

    @Schema(description = "累计退款金额（分）")
    private Long totalRefund;

    @Schema(description = "累计退款金额（元）")
    private String totalRefundYuan;
}
