package cn.liyu.hospital.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据统计 Mapper
 * <p>
 * 纯只读聚合查询，不涉及任何写操作。
 *
 * @author 医秒通
 */
@Mapper
public interface StatisticsMapper {

    // ==================== 概览统计 ====================

    Long countTodayAppointments();

    Long countTodayRevenue();

    Long countTodayNewUsers();

    Long countTodayNewCards();

    Long countTotalAppointments();

    Long countTotalRevenue();

    Long countTotalUsers();

    Long countTotalHospitals();

    Long countTotalDoctors();

    Long countTotalPlans();

    Long countTotalRefund();

    // ==================== 趋势统计 ====================

    List<Map<String, Object>> selectAppointmentTrend(@Param("days") Integer days);

    List<Map<String, Object>> selectRevenueTrend(@Param("days") Integer days);

    List<Map<String, Object>> selectRegisterTrend(@Param("days") Integer days);

    // ==================== 排行统计 ====================

    List<Map<String, Object>> selectDepartmentRank(@Param("top") Integer top);

    List<Map<String, Object>> selectDoctorRank(@Param("top") Integer top);

    // ==================== 分布统计 ====================

    List<Map<String, Object>> selectStatusDistribution();

    List<Map<String, Object>> selectPeriodDistribution();
}
