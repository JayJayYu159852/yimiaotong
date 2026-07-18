package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.dto.DashboardOverviewDTO;
import cn.liyu.hospital.dto.RankItemDTO;
import cn.liyu.hospital.dto.RevenueTrendItemDTO;
import cn.liyu.hospital.dto.TrendItemDTO;
import cn.liyu.hospital.service.IStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 · 数据统计控制器
 * <p>
 * 提供概览、趋势、排行、分布等只读统计接口，供前端 ECharts 渲染。
 *
 * @author 医秒通
 */
@Tag(name = "管理端 · 数据统计", description = "仪表盘概览 / 趋势图 / 排行 / 分布")
@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {

    @Resource
    private IStatisticsService statisticsService;

    @Operation(summary = "仪表盘概览", description = "今日挂号、今日营收、累计数据等")
    @GetMapping("/overview")
    public CommonResult<DashboardOverviewDTO> getOverview() {
        return CommonResult.success(statisticsService.getOverview());
    }

    @Operation(summary = "挂号量趋势", description = "近N天每日挂号量（折线图）")
    @GetMapping("/appointment/trend")
    public CommonResult<List<TrendItemDTO>> getAppointmentTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        return CommonResult.success(statisticsService.getAppointmentTrend(days));
    }

    @Operation(summary = "营收趋势", description = "近N天每日营收金额（折线图）")
    @GetMapping("/revenue/trend")
    public CommonResult<List<RevenueTrendItemDTO>> getRevenueTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        return CommonResult.success(statisticsService.getRevenueTrend(days));
    }

    @Operation(summary = "注册趋势", description = "近N天每日新增注册数（折线图）")
    @GetMapping("/register/trend")
    public CommonResult<List<TrendItemDTO>> getRegisterTrend(
            @RequestParam(defaultValue = "7") Integer days) {
        return CommonResult.success(statisticsService.getRegisterTrend(days));
    }

    @Operation(summary = "科室挂号排行", description = "挂号量 Top N 科室（柱状图）")
    @GetMapping("/department/rank")
    public CommonResult<List<RankItemDTO>> getDepartmentRank(
            @RequestParam(defaultValue = "10") Integer top) {
        return CommonResult.success(statisticsService.getDepartmentRank(top));
    }

    @Operation(summary = "医生热度排行", description = "挂号量 Top N 医生（柱状图）")
    @GetMapping("/doctor/rank")
    public CommonResult<List<RankItemDTO>> getDoctorRank(
            @RequestParam(defaultValue = "10") Integer top) {
        return CommonResult.success(statisticsService.getDoctorRank(top));
    }

    @Operation(summary = "预约状态分布", description = "待就诊/已爽约/已取消/已完成占比（饼图）")
    @GetMapping("/distribution/status")
    public CommonResult<List<RankItemDTO>> getStatusDistribution() {
        return CommonResult.success(statisticsService.getStatusDistribution());
    }

    @Operation(summary = "时段分布", description = "上午/下午挂号量占比（饼图）")
    @GetMapping("/distribution/period")
    public CommonResult<List<RankItemDTO>> getPeriodDistribution() {
        return CommonResult.success(statisticsService.getPeriodDistribution());
    }

    @Operation(summary = "一键获取全部统计数据", description = "一次性返回所有图表数据，减少前端请求次数")
    @GetMapping("/all")
    public CommonResult<Map<String, Object>> getAllStatistics(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "10") Integer top) {
        Map<String, Object> result = new HashMap<>();
        result.put("overview", statisticsService.getOverview());
        result.put("appointmentTrend", statisticsService.getAppointmentTrend(days));
        result.put("revenueTrend", statisticsService.getRevenueTrend(days));
        result.put("registerTrend", statisticsService.getRegisterTrend(days));
        result.put("departmentRank", statisticsService.getDepartmentRank(top));
        result.put("doctorRank", statisticsService.getDoctorRank(top));
        result.put("statusDistribution", statisticsService.getStatusDistribution());
        result.put("periodDistribution", statisticsService.getPeriodDistribution());
        return CommonResult.success(result);
    }

    @Operation(summary = "导出数据统计报表", description = "导出全部统计数据为 Excel（近30天趋势 + Top10 排行）")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        statisticsService.exportStatistics(response);
    }
}
