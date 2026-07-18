package cn.liyu.hospital.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * UV（独立访客）统计组件 — 基于 Redis HyperLogLog
 * <p>
 * 使用场景：
 * - 统计每家医院详情页的当日独立访客数
 * - 统计每位医生主页的浏览量
 * - 统计科室列表页访问量
 * <p>
 * HyperLogLog 优势：极低内存（12KB/Key），0.81%误差，适合UV统计
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Component
public class UvStatisticsComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(UvStatisticsComponent.class);

    private static final String UV_HOSPITAL_KEY = "uv:hospital:";
    private static final String UV_DOCTOR_KEY = "uv:doctor:";
    private static final String UV_SPECIAL_KEY = "uv:special:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 记录医院详情页 UV（同时写今日 + 总计）
     */
    public void recordHospitalUV(Long hospitalId, Long userId) {
        String keyToday = UV_HOSPITAL_KEY + hospitalId + ":" + today();
        String keyTotal = UV_HOSPITAL_KEY + hospitalId + ":total";
        String value = String.valueOf(userId);
        stringRedisTemplate.opsForHyperLogLog().add(keyToday, value);
        stringRedisTemplate.opsForHyperLogLog().add(keyTotal, value);
    }

    /**
     * 记录医生主页 UV（同时写今日 + 总计）
     */
    public void recordDoctorUV(Long doctorId, Long userId) {
        String keyToday = UV_DOCTOR_KEY + doctorId + ":" + today();
        String keyTotal = UV_DOCTOR_KEY + doctorId + ":total";
        String value = String.valueOf(userId);
        stringRedisTemplate.opsForHyperLogLog().add(keyToday, value);
        stringRedisTemplate.opsForHyperLogLog().add(keyTotal, value);
    }

    /**
     * 记录科室访问 UV（同时写今日 + 总计）
     */
    public void recordSpecialUV(Long specialId, Long userId) {
        String keyToday = UV_SPECIAL_KEY + specialId + ":" + today();
        String keyTotal = UV_SPECIAL_KEY + specialId + ":total";
        String value = String.valueOf(userId);
        stringRedisTemplate.opsForHyperLogLog().add(keyToday, value);
        stringRedisTemplate.opsForHyperLogLog().add(keyTotal, value);
    }

    /**
     * 查询医院当日 UV
     */
    public long getHospitalUV(Long hospitalId) {
        String key = UV_HOSPITAL_KEY + hospitalId + ":" + today();
        return stringRedisTemplate.opsForHyperLogLog().size(key);
    }

    /**
     * 查询医生当日 UV
     */
    public long getDoctorUV(Long doctorId) {
        String key = UV_DOCTOR_KEY + doctorId + ":" + today();
        return stringRedisTemplate.opsForHyperLogLog().size(key);
    }

    /**
     * 查询科室当日 UV
     */
    public long getSpecialUV(Long specialId) {
        String key = UV_SPECIAL_KEY + specialId + ":" + today();
        return stringRedisTemplate.opsForHyperLogLog().size(key);
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
