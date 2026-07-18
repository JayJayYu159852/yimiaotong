package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.dto.DashboardOverviewDTO;
import cn.liyu.hospital.dto.RankItemDTO;
import cn.liyu.hospital.dto.RevenueTrendItemDTO;
import cn.liyu.hospital.dto.TrendItemDTO;
import cn.liyu.hospital.mapper.StatisticsMapper;
import cn.liyu.hospital.service.IStatisticsService;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 数据统计服务实现
 * <p>
 * 全部为只读查询，零写入。
 *
 * @author 医秒通
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    @Resource
    private StatisticsMapper statisticsMapper;

    @Override
    public DashboardOverviewDTO getOverview() {
        DashboardOverviewDTO dto = new DashboardOverviewDTO();

        // 今日数据
        dto.setTodayAppointments(optLong(statisticsMapper.countTodayAppointments()));
        dto.setTodayRevenue(optLong(statisticsMapper.countTodayRevenue()));
        dto.setTodayRevenueYuan(formatYuan(dto.getTodayRevenue()));
        dto.setTodayNewUsers(optLong(statisticsMapper.countTodayNewUsers()));
        dto.setTodayNewCards(optLong(statisticsMapper.countTodayNewCards()));

        // 累计数据
        dto.setTotalAppointments(optLong(statisticsMapper.countTotalAppointments()));
        dto.setTotalRevenue(optLong(statisticsMapper.countTotalRevenue()));
        dto.setTotalRevenueYuan(formatYuan(dto.getTotalRevenue()));
        dto.setTotalUsers(optLong(statisticsMapper.countTotalUsers()));
        dto.setTotalHospitals(optLong(statisticsMapper.countTotalHospitals()));
        dto.setTotalDoctors(optLong(statisticsMapper.countTotalDoctors()));
        dto.setTotalPlans(optLong(statisticsMapper.countTotalPlans()));
        dto.setTotalRefund(optLong(statisticsMapper.countTotalRefund()));
        dto.setTotalRefundYuan(formatYuan(dto.getTotalRefund()));

        return dto;
    }

    @Override
    public List<TrendItemDTO> getAppointmentTrend(Integer days) {
        if (days == null || days <= 0) days = 7;
        return fillTrendDates(statisticsMapper.selectAppointmentTrend(days), days);
    }

    @Override
    public List<RevenueTrendItemDTO> getRevenueTrend(Integer days) {
        if (days == null || days <= 0) days = 7;
        List<RevenueTrendItemDTO> result = new ArrayList<>();
        List<Map<String, Object>> rows = statisticsMapper.selectRevenueTrend(days);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                RevenueTrendItemDTO item = new RevenueTrendItemDTO();
                item.setDate(objStr(row.get("date")));
                item.setAmount(optLong(row.get("amount")));
                item.setAmountYuan(formatYuan(item.getAmount()));
                result.add(item);
            }
        }
        return fillRevenueTrendDates(result, days);
    }

    @Override
    public List<TrendItemDTO> getRegisterTrend(Integer days) {
        if (days == null || days <= 0) days = 7;
        return fillTrendDates(statisticsMapper.selectRegisterTrend(days), days);
    }

    @Override
    public List<RankItemDTO> getDepartmentRank(Integer top) {
        if (top == null || top <= 0) top = 10;
        List<Map<String, Object>> rows = statisticsMapper.selectDepartmentRank(top);
        return buildRankList(rows);
    }

    @Override
    public List<RankItemDTO> getDoctorRank(Integer top) {
        if (top == null || top <= 0) top = 10;
        List<Map<String, Object>> rows = statisticsMapper.selectDoctorRank(top);
        return buildRankList(rows);
    }

    @Override
    public List<RankItemDTO> getStatusDistribution() {
        List<Map<String, Object>> rows = statisticsMapper.selectStatusDistribution();
        return buildRankList(rows);
    }

    @Override
    public List<RankItemDTO> getPeriodDistribution() {
        List<Map<String, Object>> rows = statisticsMapper.selectPeriodDistribution();
        return buildRankList(rows);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 将 Map 行数据转换为 RankItemDTO 列表并计算百分比
     */
    private List<RankItemDTO> buildRankList(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<RankItemDTO> list = new ArrayList<>();
        long total = 0;

        // 第一遍：提取数据并计算总和
        List<String> names = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = objStr(row.get("name"));
            Long value = optLong(row.get("value"));
            names.add(name);
            values.add(value);
            total += value;
        }

        // 第二遍：计算百分比
        long finalTotal = total;
        for (int i = 0; i < names.size(); i++) {
            RankItemDTO item = new RankItemDTO();
            item.setName(names.get(i));
            item.setValue(values.get(i));
            if (finalTotal > 0) {
                double pct = BigDecimal.valueOf(values.get(i))
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(finalTotal), 1, RoundingMode.HALF_UP)
                        .doubleValue();
                item.setPercentage(pct);
            } else {
                item.setPercentage(0.0);
            }
            list.add(item);
        }

        return list;
    }

    /**
     * 填充趋势日期（补全缺失日期，日期缺失的填0）
     */
    private List<TrendItemDTO> fillTrendDates(List<Map<String, Object>> rows, Integer days) {
        List<TrendItemDTO> result = new ArrayList<>();
        Set<String> dateSet = new HashSet<>();

        // 从查询结果中读取数据
        Map<String, Long> dataMap = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String date = objStr(row.get("date"));
                Long count = optLong(row.get("count"));
                dataMap.put(date, count);
                dateSet.add(date);
            }
        }

        // 补全日期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < days; i++) {
            String dateStr = sdf.format(cal.getTime());
            TrendItemDTO item = new TrendItemDTO();
            item.setDate(dateStr);
            item.setCount(dataMap.getOrDefault(dateStr, 0L));
            result.add(item);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return result;
    }

    /**
     * 填充营收趋势日期
     */
    private List<RevenueTrendItemDTO> fillRevenueTrendDates(List<RevenueTrendItemDTO> items, Integer days) {
        List<RevenueTrendItemDTO> result = new ArrayList<>();
        Map<String, RevenueTrendItemDTO> dataMap = new LinkedHashMap<>();
        if (items != null) {
            for (RevenueTrendItemDTO item : items) {
                dataMap.put(item.getDate(), item);
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < days; i++) {
            String dateStr = sdf.format(cal.getTime());
            RevenueTrendItemDTO item = dataMap.getOrDefault(dateStr, new RevenueTrendItemDTO());
            if (item.getDate() == null) {
                item.setDate(dateStr);
                item.setAmount(0L);
                item.setAmountYuan("0.00");
            }
            result.add(item);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return result;
    }

    /**
     * 将分转换为元格式字符串
     */
    private String formatYuan(Long fen) {
        if (fen == null) return "0.00";
        return BigDecimal.valueOf(fen)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .toString();
    }

    /**
     * 安全获取 Long 值（处理 null）
     */
    private Long optLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 将对象安全转为字符串
     */
    private String objStr(Object obj) {
        if (obj == null) return "";
        // java.sql.Date / java.util.Date 的处理
        if (obj instanceof java.util.Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format((java.util.Date) obj);
        }
        return obj.toString();
    }

    // ==================== 报表导出（Apache POI，对标苍穹外卖 exportBusinessData） ====================

    @Override
    public void exportStatistics(HttpServletResponse response) {
        // 1. 查询全部统计数据（近30天趋势 + Top10 排行，全部复用现有只读查询）
        DashboardOverviewDTO overview = getOverview();
        List<TrendItemDTO> appointmentTrend = getAppointmentTrend(30);
        List<RevenueTrendItemDTO> revenueTrend = getRevenueTrend(30);
        List<TrendItemDTO> registerTrend = getRegisterTrend(30);
        List<RankItemDTO> departmentRank = getDepartmentRank(10);
        List<RankItemDTO> doctorRank = getDoctorRank(10);
        List<RankItemDTO> statusDistribution = getStatusDistribution();
        List<RankItemDTO> periodDistribution = getPeriodDistribution();

        // 2. 通过 POI 将数据写入 Excel（代码构建工作簿，多 Sheet）
        try (XSSFWorkbook excel = new XSSFWorkbook()) {
            XSSFCellStyle headerStyle = excel.createCellStyle();
            XSSFFont font = excel.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // 概览 Sheet
            fillOverviewSheet(excel, headerStyle, overview);
            // 趋势 Sheet
            fillTrendSheet(excel, headerStyle, "挂号量趋势", "挂号量", appointmentTrend);
            fillRevenueSheet(excel, headerStyle, revenueTrend);
            fillTrendSheet(excel, headerStyle, "注册趋势", "新增注册数", registerTrend);
            // 排行/分布 Sheet
            fillRankSheet(excel, headerStyle, "科室挂号排行", "科室", departmentRank);
            fillRankSheet(excel, headerStyle, "医生热度排行", "医生", doctorRank);
            fillRankSheet(excel, headerStyle, "预约状态分布", "状态", statusDistribution);
            fillRankSheet(excel, headerStyle, "时段分布", "时段", periodDistribution);

            // 3. 通过输出流将 Excel 文件下载到客户端浏览器
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("数据统计报表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.close();
        } catch (IOException e) {
            LOGGER.error("统计报表导出失败", e);
        }
    }

    /**
     * 填充概览 Sheet（指标名 + 数值 两列）
     */
    private void fillOverviewSheet(XSSFWorkbook excel, XSSFCellStyle headerStyle, DashboardOverviewDTO overview) {
        XSSFSheet sheet = excel.createSheet("概览");
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 20 * 256);

        XSSFRow title = sheet.createRow(0);
        title.createCell(0).setCellValue("数据统计报表");
        title.getCell(0).setCellStyle(headerStyle);
        title.createCell(1).setCellValue("导出时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        String[][] items = {
                {"今日挂号量", objStr(overview.getTodayAppointments())},
                {"今日营收(元)", objStr(overview.getTodayRevenueYuan())},
                {"今日新增用户", objStr(overview.getTodayNewUsers())},
                {"今日新增就诊卡", objStr(overview.getTodayNewCards())},
                {"累计挂号量", objStr(overview.getTotalAppointments())},
                {"累计营收(元)", objStr(overview.getTotalRevenueYuan())},
                {"累计退款(元)", objStr(overview.getTotalRefundYuan())},
                {"累计用户数", objStr(overview.getTotalUsers())},
                {"医院数", objStr(overview.getTotalHospitals())},
                {"医生数", objStr(overview.getTotalDoctors())},
                {"出诊计划数", objStr(overview.getTotalPlans())}
        };

        XSSFRow header = sheet.createRow(2);
        header.createCell(0).setCellValue("指标");
        header.createCell(1).setCellValue("数值");
        header.getCell(0).setCellStyle(headerStyle);
        header.getCell(1).setCellStyle(headerStyle);

        for (int i = 0; i < items.length; i++) {
            XSSFRow row = sheet.createRow(3 + i);
            row.createCell(0).setCellValue(items[i][0]);
            row.createCell(1).setCellValue(items[i][1]);
        }
    }

    /**
     * 填充趋势 Sheet（日期 + 数量 两列）
     */
    private void fillTrendSheet(XSSFWorkbook excel, XSSFCellStyle headerStyle, String sheetName,
                                String countTitle, List<TrendItemDTO> list) {
        XSSFSheet sheet = excel.createSheet(sheetName);
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 15 * 256);

        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue(countTitle);
        header.getCell(0).setCellStyle(headerStyle);
        header.getCell(1).setCellStyle(headerStyle);

        int r = 1;
        for (TrendItemDTO item : list) {
            XSSFRow row = sheet.createRow(r++);
            row.createCell(0).setCellValue(objStr(item.getDate()));
            row.createCell(1).setCellValue(item.getCount() == null ? 0 : item.getCount());
        }
    }

    /**
     * 填充营收趋势 Sheet（日期 + 金额分 + 金额元 三列）
     */
    private void fillRevenueSheet(XSSFWorkbook excel, XSSFCellStyle headerStyle, List<RevenueTrendItemDTO> list) {
        XSSFSheet sheet = excel.createSheet("营收趋势");
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 15 * 256);

        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        header.createCell(1).setCellValue("营收(分)");
        header.createCell(2).setCellValue("营收(元)");
        for (int i = 0; i <= 2; i++) header.getCell(i).setCellStyle(headerStyle);

        int r = 1;
        for (RevenueTrendItemDTO item : list) {
            XSSFRow row = sheet.createRow(r++);
            row.createCell(0).setCellValue(objStr(item.getDate()));
            row.createCell(1).setCellValue(item.getAmount() == null ? 0 : item.getAmount());
            row.createCell(2).setCellValue(objStr(item.getAmountYuan()));
        }
    }

    /**
     * 填充排行/分布 Sheet（名称 + 数量 + 占比 三列）
     */
    private void fillRankSheet(XSSFWorkbook excel, XSSFCellStyle headerStyle, String sheetName,
                               String nameTitle, List<RankItemDTO> list) {
        XSSFSheet sheet = excel.createSheet(sheetName);
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 15 * 256);

        XSSFRow header = sheet.createRow(0);
        header.createCell(0).setCellValue(nameTitle);
        header.createCell(1).setCellValue("数量");
        header.createCell(2).setCellValue("占比(%)");
        for (int i = 0; i <= 2; i++) header.getCell(i).setCellStyle(headerStyle);

        int r = 1;
        for (RankItemDTO item : list) {
            XSSFRow row = sheet.createRow(r++);
            row.createCell(0).setCellValue(objStr(item.getName()));
            row.createCell(1).setCellValue(item.getValue() == null ? 0 : item.getValue());
            row.createCell(2).setCellValue(item.getPercentage() == null ? 0 : item.getPercentage());
        }
    }
}
