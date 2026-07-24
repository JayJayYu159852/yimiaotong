package cn.liyu.hospital.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置（L1 进程内缓存）
 * <p>
 * 与 Redis（L2 分布式缓存）组成多级缓存架构：
 * - L1 Caffeine：纳秒级延迟，10min 过期，最大 10_000 条
 * - L2 Redis：毫秒级延迟，业务自定义 TTL
 * <p>
 * 对标黑马点评多级缓存练习，写入策略：先写 Redis → 再写 Caffeine
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Configuration
public class CaffeineConfig {

    /**
     * 本地缓存最大条目数
     */
    private static final int MAX_SIZE = 10_000;

    /**
     * 本地缓存过期时间（分钟）
     */
    private static final int TTL_MINUTES = 10;

    /**
     * 业务对象本地缓存（hospital_info、doctor_info、special_info 等）
     */
    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
