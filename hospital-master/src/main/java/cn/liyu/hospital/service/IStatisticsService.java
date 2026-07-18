package cn.liyu.hospital.service;

import cn.liyu.hospital.dto.DashboardOverviewDTO;
import cn.liyu.hospital.dto.RankItemDTO;
import cn.liyu.hospital.dto.RevenueTrendItemDTO;
import cn.liyu.hospital.dto.TrendItemDTO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据统计服务接口
 * <p>
 * 纯只读聚合查询，不涉及任何数据写操作。
 *
 * @author 医秒通
 */
public interface IStatisticsService {

    /**
     * 获取仪表盘概览数据
     */
    DashboardOverviewDTO getOverview();

    /**
     * 获取挂号量趋势
     *
     * @param days 近N天
     */
    List<TrendItemDTO> getAppointmentTrend(Integer days);

    /**
     * 获取营收趋势
     *
     * @param days 近N天
     */
    List<RevenueTrendItemDTO> getRevenueTrend(Integer days);

    /**
     * 获取注册趋势
     *
     * @param days 近N天
     */
    List<TrendItemDTO> getRegisterTrend(Integer days);

    /**
     * 获取科室挂号排行
     *
     * @param top Top N
     */
    List<RankItemDTO> getDepartmentRank(Integer top);

    /**
     * 获取医生热度排行
     *
     * @param top Top N
     */
    List<RankItemDTO> getDoctorRank(Integer top);

    /**
     * 获取预约状态分布
     */
    List<RankItemDTO> getStatusDistribution();

    /**
     * 获取上下午时段分布
     */
    List<RankItemDTO> getPeriodDistribution();

    /**
     * 导出全部统计数据报表（Excel）
     *
     * @param response HTTP响应，Excel通过输出流写回客户端
     */
    void exportStatistics(HttpServletResponse response);
}
