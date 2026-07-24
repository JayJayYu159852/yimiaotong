package cn.liyu.hospital.component;

import cn.liyu.hospital.entity.HospitalDoctor;
import cn.liyu.hospital.entity.HospitalDoctorExample;
import cn.liyu.hospital.entity.HospitalInfo;
import cn.liyu.hospital.entity.HospitalInfoExample;
import cn.liyu.hospital.entity.HospitalSpecial;
import cn.liyu.hospital.entity.HospitalSpecialExample;
import cn.liyu.hospital.mapper.HospitalDoctorMapper;
import cn.liyu.hospital.mapper.HospitalInfoMapper;
import cn.liyu.hospital.mapper.HospitalSpecialMapper;
import cn.liyu.hospital.service.IHospitalSpecialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 缓存预热（应用启动时执行）
 * <p>
 * 预热范围：
 * <ol>
 *   <li>专科列表首页（逻辑过期缓存预热）</li>
 *   <li>布隆过滤器（将现有医院/医生/专科 ID 注册到 Redisson RBloomFilter）</li>
 * </ol>
 * 对标黑马点评：逻辑过期缓存需提前预热，布隆过滤器需初始注册。
 *
 * @author 医秒通
 */
@Component
public class CacheWarmUpRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheWarmUpRunner.class);

    private static final int WARM_UP_PAGE_NUM = 1;
    private static final int WARM_UP_PAGE_SIZE = 10;

    @Resource
    private IHospitalSpecialService specialService;

    @Resource
    private BloomFilterService bloomFilter;

    @Resource
    private HospitalInfoMapper hospitalInfoMapper;

    @Resource
    private HospitalDoctorMapper hospitalDoctorMapper;

    @Resource
    private HospitalSpecialMapper hospitalSpecialMapper;

    @Override
    public void run(ApplicationArguments args) {
        warmUpCache();
        warmUpBloomFilters();
    }

    private void warmUpCache() {
        try {
            specialService.list((String) null, WARM_UP_PAGE_NUM, WARM_UP_PAGE_SIZE);
            LOGGER.info("缓存预热完成：专科列表首页");
        } catch (Exception e) {
            LOGGER.warn("缓存预热失败，跳过（不影响启动）", e);
        }
    }

    /**
     * 布隆过滤器初始注册：将数据库中已有数据的 ID 批量注册到布隆过滤器
     */
    private void warmUpBloomFilters() {
        try {
            List<HospitalInfo> hospitals = hospitalInfoMapper.selectByExample(new HospitalInfoExample());
            for (HospitalInfo h : hospitals) {
                bloomFilter.addHospital(h.getId());
            }
            LOGGER.info("布隆过滤器预热完成：医院 {} 条", hospitals.size());
        } catch (Exception e) {
            LOGGER.warn("医院布隆过滤器预热失败，跳过", e);
        }

        try {
            List<HospitalDoctor> doctors = hospitalDoctorMapper.selectByExample(new HospitalDoctorExample());
            for (HospitalDoctor d : doctors) {
                bloomFilter.addDoctor(d.getId());
            }
            LOGGER.info("布隆过滤器预热完成：医生 {} 条", doctors.size());
        } catch (Exception e) {
            LOGGER.warn("医生布隆过滤器预热失败，跳过", e);
        }

        try {
            List<HospitalSpecial> specials = hospitalSpecialMapper.selectByExample(new HospitalSpecialExample());
            for (HospitalSpecial s : specials) {
                bloomFilter.addSpecial(s.getId());
            }
            LOGGER.info("布隆过滤器预热完成：专科 {} 条", specials.size());
        } catch (Exception e) {
            LOGGER.warn("专科布隆过滤器预热失败，跳过", e);
        }
    }
}
