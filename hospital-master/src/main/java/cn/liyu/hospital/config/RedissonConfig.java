package cn.liyu.hospital.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置
 * <p>
 * 基于 spring.redis.* 配置自动连接，支持：
 * - RLock：可重入分布式锁（秒杀防超卖、预约防重复）
 * - RReadWriteLock：读写锁
 * - RSemaphore：信号量（号源控制）
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setDatabase(redisDatabase)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10);

        return Redisson.create(config);
    }
}
