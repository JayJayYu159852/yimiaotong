package cn.liyu.hospital.component;

import cn.liyu.hospital.entity.VisitPlan;
import cn.liyu.hospital.entity.VisitPlanExample;
import cn.liyu.hospital.mapper.VisitPlanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 启动时自动清理过期出诊计划 + Redis 缓存
 * <p>
 * 不再负责生成计划，生成逻辑由 {@link SchedulePlanGenerator} 定时任务接管。
 *
 * @author 医秒通
 */
@Component
@Order(1)
public class DemoPlanInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPlanInitializer.class);

    @Resource
    private VisitPlanMapper planMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            LocalDate today = LocalDate.now();
            Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

            // 删除今天之前的过期计划 + Redis 缓存
            VisitPlanExample expiredExample = new VisitPlanExample();
            expiredExample.createCriteria().andDayLessThan(todayStart);
            List<VisitPlan> expiredPlans = planMapper.selectByExample(expiredExample);

            if (expiredPlans.isEmpty()) {
                log.info("无过期出诊计划需要清理");
                return;
            }

            for (VisitPlan plan : expiredPlans) {
                stringRedisTemplate.delete("seckill:stock:" + plan.getId());
                stringRedisTemplate.delete("seckill:order:" + plan.getId());
            }
            int deleted = planMapper.deleteByExample(expiredExample);
            log.info("自动清理完成：删除 {} 条过期出诊计划及对应Redis缓存", deleted);

        } catch (Exception e) {
            log.warn("过期计划清理失败，跳过（不影响启动）", e);
        }
    }
}
