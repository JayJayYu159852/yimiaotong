package cn.liyu.hospital.component;

import cn.liyu.hospital.service.IHospitalSpecialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 缓存预热（应用启动时执行）
 * <p>
 * 对标黑马点评：逻辑过期方案的缓存需要提前预热（黑马在测试类中手动调用
 * setWithLogicalExpire 预热，此处改为启动自动预热，更符合生产实践）。
 * <p>
 * 预热范围：专科列表的首页（挂号主链路最高频入口）。
 * 未预热到的分页参数组合由 queryPageWithLogicalExpire 的自预热机制兜底。
 *
 * @author 医秒通
 */
@Component
public class CacheWarmUpRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheWarmUpRunner.class);

    /**
     * 预热的默认页参数（与前端首页默认请求保持一致）
     */
    private static final int WARM_UP_PAGE_NUM = 1;

    private static final int WARM_UP_PAGE_SIZE = 10;

    @Resource
    private IHospitalSpecialService specialService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 走缓存查询路径即完成预热（未命中时同步加载并写入逻辑过期缓存）
            specialService.list((String) null, WARM_UP_PAGE_NUM, WARM_UP_PAGE_SIZE);
            LOGGER.info("缓存预热完成：专科列表首页");
        } catch (Exception e) {
            // 预热失败不影响应用启动（如 Redis 暂不可用），后续请求自预热兜底
            LOGGER.warn("缓存预热失败，跳过（不影响启动）", e);
        }
    }
}
