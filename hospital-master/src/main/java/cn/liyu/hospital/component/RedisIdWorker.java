package cn.liyu.hospital.component;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一ID生成器（基于Redis自增）
 * <p>
 * ID结构：64位Long
 * - 高31位：时间戳（从2022-01-01 00:00:00开始，可用约69年）
 * - 低32位：Redis自增序列号（每天重置）
 * <p>
 * 使用场景：秒杀预约记录ID、普通预约记录ID、消息ID
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Component
public class RedisIdWorker {

    /**
     * 开始时间戳（2022-01-01 00:00:00）
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成下一个ID
     *
     * @param keyPrefix 业务前缀（如 "appointment"）
     * @return 全局唯一ID
     */
    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 2.1 获取当前日期（精确到天）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2 Redis自增
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接并返回（时间戳左移32位 | 序列号）
        return timestamp << COUNT_BITS | count;
    }
}
