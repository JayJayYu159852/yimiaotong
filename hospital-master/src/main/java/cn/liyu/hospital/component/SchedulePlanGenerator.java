package cn.liyu.hospital.component;

import cn.liyu.hospital.entity.HospitalDoctor;
import cn.liyu.hospital.entity.HospitalDoctorExample;
import cn.liyu.hospital.entity.VisitPlan;
import cn.liyu.hospital.entity.VisitPlanExample;
import cn.liyu.hospital.mapper.HospitalDoctorMapper;
import cn.liyu.hospital.mapper.VisitPlanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 每天 00:00 自动生成第7天（今天+6）的出诊计划 + Redis号源库存
 * <p>
 * 逻辑极简：只补尾部一天。已存在的计划 / 库存完全不被触碰。
 *
 * @author 医秒通
 */
@Component
@Order(2)
public class SchedulePlanGenerator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchedulePlanGenerator.class);

    /** 每个计划的号源库存 */
    private static final int STOCK_PER_PLAN = 10;

    /**
     * 医生 → 医院ID 映射（匹配 hospital_doctor 表中实际存在的医生）
     */
    private static final Map<Long, Long> DOCTOR_HOSPITAL_MAP = Map.of(
            10000L, 1000L,
            10001L, 1000L,
            10002L, 1000L,
            10003L, 1000L,
            10004L, 1000L,
            10005L, 1000L,
            10006L, 1001L,
            10007L, 1001L,
            10008L, 1001L,
            10009L, 1007L
    );

    @Resource
    private VisitPlanMapper planMapper;

    @Resource
    private HospitalDoctorMapper doctorMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 启动时立即执行一次（不等凌晨）
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("启动时检查7天窗口计划完整性...");
        generateTailPlan();
    }

    /**
     * 每天 00:00 执行：先清理过期计划，再遍历7天窗口，哪天缺计划就补哪天
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateTailPlan() {
        try {
            LocalDate today = LocalDate.now();

            // ========== 0. 清理今天之前的过期计划 + Redis 缓存 ==========
            cleanExpiredPlans(today);

            // ========== 1. 加载医生数据 ==========
            List<HospitalDoctor> doctors = doctorMapper.selectByExample(new HospitalDoctorExample());

            // ========== 2. 遍历7天窗口，缺哪补哪 ==========
            int totalCreated = 0;
            for (int i = 0; i < 7; i++) {
                LocalDate targetDay = today.plusDays(i);
                Date targetDate = Date.from(targetDay.atStartOfDay(ZoneId.systemDefault()).toInstant());

                VisitPlanExample example = new VisitPlanExample();
                example.createCriteria()
                        .andDayGreaterThanOrEqualTo(targetDate)
                        .andDayLessThanOrEqualTo(targetDate);
                long count = planMapper.countByExample(example);

                if (count > 0) {
                    continue; // 已有计划 → 跳过
                }

                // 该天无计划 → 生成（每个医生每天仅1条）
                int created = 0;
                for (HospitalDoctor doctor : doctors) {
                    Long hospitalId = DOCTOR_HOSPITAL_MAP.get(doctor.getId());
                    if (hospitalId == null) {
                        continue; // 未配置映射的医生跳过
                    }

                    VisitPlan plan = new VisitPlan();
                    plan.setHospitalId(hospitalId);
                    plan.setSpecialId(doctor.getSpecialId());
                    plan.setDoctorId(doctor.getId());
                    plan.setTime(1); // 上午
                    plan.setDay(targetDate);
                    plan.setGmtCreate(new Date());
                    plan.setGmtModified(new Date());

                    planMapper.insertSelective(plan);

                    stringRedisTemplate.opsForValue().set(
                            "seckill:stock:" + plan.getId(),
                            String.valueOf(STOCK_PER_PLAN)
                    );
                    created++;
                }
                if (created > 0) {
                    log.info("{} 新增 {} 条出诊计划，stock={}", targetDay, created, STOCK_PER_PLAN);
                    totalCreated += created;
                }
            }

            if (totalCreated == 0) {
                log.info("7天窗口计划完整，无需生成");
            } else {
                log.info("定时生成完成：本次共新增 {} 条计划", totalCreated);
            }

        } catch (Exception e) {
            log.warn("定时生成计划失败，下次重试", e);
        }
    }

    /**
     * 清理今天之前的过期出诊计划 + 对应 Redis 秒杀缓存
     *
     * @param today 今天日期
     */
    private void cleanExpiredPlans(LocalDate today) {
        try {
            Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

            VisitPlanExample expiredExample = new VisitPlanExample();
            expiredExample.createCriteria().andDayLessThan(todayStart);
            List<VisitPlan> expiredPlans = planMapper.selectByExample(expiredExample);

            if (expiredPlans.isEmpty()) {
                return;
            }

            for (VisitPlan plan : expiredPlans) {
                stringRedisTemplate.delete("seckill:stock:" + plan.getId());
                stringRedisTemplate.delete("seckill:order:" + plan.getId());
            }
            int deleted = planMapper.deleteByExample(expiredExample);
            log.info("定时清理完成：删除 {} 条过期出诊计划及对应Redis缓存", deleted);

        } catch (Exception e) {
            log.warn("过期计划清理失败，跳过（不影响生成）", e);
        }
    }
}
