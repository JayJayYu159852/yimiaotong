package cn.liyu.hospital.component;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Redisson 布隆过滤器服务（防缓存穿透第一道防线）
 * <p>
 * 为每种业务实体维护独立布隆过滤器，ID 在数据写入时注册，查询时先判存在性。
 * Redis Bitmap 实现，内存极省（~1MB/千万级Key @ 1%误判率）。
 * <p>
 * 误判率 1%：布隆返回"可能存在"时仍需查 Redis/DB；返回"一定不存在"时直接短路返回 null。
 * 对标黑马点评 + 苍穹外卖缓存防护体系。
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Component
public class BloomFilterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BloomFilterService.class);

    /** 预期数据量（根据业务估算） */
    private static final long EXPECTED_HOSPITALS = 5_000L;
    private static final long EXPECTED_DOCTORS = 50_000L;
    private static final long EXPECTED_SPECIALS = 1_000L;
    private static final long EXPECTED_NOTICES = 10_000L;

    /** 误判率 1% */
    private static final double FALSE_PROBABILITY = 0.01;

    @Resource
    private RedissonClient redissonClient;

    private RBloomFilter<String> hospitalFilter;
    private RBloomFilter<String> doctorFilter;
    private RBloomFilter<String> specialFilter;
    private RBloomFilter<String> noticeFilter;

    @PostConstruct
    public void init() {
        hospitalFilter = initFilter("bloom:hospital", EXPECTED_HOSPITALS);
        doctorFilter = initFilter("bloom:doctor", EXPECTED_DOCTORS);
        specialFilter = initFilter("bloom:special", EXPECTED_SPECIALS);
        noticeFilter = initFilter("bloom:notice", EXPECTED_NOTICES);
        LOGGER.info("布隆过滤器初始化完成: hospital/doctor/special/notice");
    }

    private RBloomFilter<String> initFilter(String name, long expectedInsertions) {
        RBloomFilter<String> filter = redissonClient.getBloomFilter(name);
        filter.tryInit(expectedInsertions, FALSE_PROBABILITY);
        return filter;
    }

    // ==================== 查询方法 ====================

    /** 医院 ID 可能存在？false = 一定不存在 */
    public boolean mightContainHospital(Long id) {
        return hospitalFilter.contains(String.valueOf(id));
    }

    /** 医生 ID 可能存在？ */
    public boolean mightContainDoctor(Long id) {
        return doctorFilter.contains(String.valueOf(id));
    }

    /** 专科 ID 可能存在？ */
    public boolean mightContainSpecial(Long id) {
        return specialFilter.contains(String.valueOf(id));
    }

    /** 资讯 ID 可能存在？ */
    public boolean mightContainNotice(Long id) {
        return noticeFilter.contains(String.valueOf(id));
    }

    // ==================== 注册方法（数据写入时调用） ====================

    public void addHospital(Long id) {
        hospitalFilter.add(String.valueOf(id));
    }

    public void addDoctor(Long id) {
        doctorFilter.add(String.valueOf(id));
    }

    public void addSpecial(Long id) {
        specialFilter.add(String.valueOf(id));
    }

    public void addNotice(Long id) {
        noticeFilter.add(String.valueOf(id));
    }
}
