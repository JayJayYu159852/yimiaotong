package cn.liyu.hospital.component;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.pagehelper.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 缓存工具客户端（封装缓存穿透/击穿/雪崩解决方案）
 * <p>
 * 对标黑马点评 CacheClient，新增多级缓存架构：
 * <ul>
 *   <li>L1 Caffeine 本地缓存：纳秒级，10min 过期</li>
 *   <li>布隆过滤器：拦截一定不存在的 Key，防穿透第一道防线</li>
 *   <li>L2 Redis 分布式缓存：毫秒级，业务自定义 TTL</li>
 *   <li>空值缓存：防穿透第二道防线</li>
 *   <li>互斥锁 / 逻辑过期：防击穿</li>
 *   <li>随机 TTL：防雪崩</li>
 * </ul>
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Component
public class CacheClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheClient.class);

    /**
     * 空值 TTL（2分钟）
     */
    private static final long CACHE_NULL_TTL = 2L;

    /**
     * 互斥锁 TTL（10秒）
     */
    private static final long LOCK_TTL = 10L;

    /**
     * 互斥锁获取失败最大重试次数（约 20 * 50ms = 1秒，超限降级直查数据库，防线程无限空转堆积）
     */
    private static final int MUTEX_MAX_RETRY = 20;

    /**
     * 互斥锁重试间隔（毫秒）
     */
    private static final long MUTEX_RETRY_INTERVAL_MS = 50L;

    /**
     * TTL 随机抖动源（防雪崩：同一批 key 过期时间打散，避免同时失效打垮数据库）
     */
    private static final Random RANDOM = new Random();

    /**
     * 缓存重建线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * L1 本地缓存（Caffeine，进程内），优先于 L2 Redis
     */
    @Resource
    private Cache<String, Object> localCache;

    /**
     * Redisson 布隆过滤器，拦截一定不存在的 Key（防穿透第一道防线）
     */
    @Resource
    private BloomFilterService bloomFilter;

    /**
     * 应用关闭时优雅关闭缓存重建线程池
     */
    @PreDestroy
    public void shutdown() {
        CACHE_REBUILD_EXECUTOR.shutdown();
        try {
            if (!CACHE_REBUILD_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                CACHE_REBUILD_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            CACHE_REBUILD_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 缓存穿透 — 空值缓存（多级：Caffeine → 布隆 → Redis → DB） ====================

    /**
     * 缓存穿透防护（Caffeine L1 → 布隆 → Redis L2 → DB）
     * <p>
     * 查询链路：
     * <ol>
     *   <li>查 Caffeine 本地缓存（命中直接返回）</li>
     *   <li>布隆过滤器判断（一定不存在 → 直接返回 null，1% 误判放过由空值缓存兜底）</li>
     *   <li>查 Redis（命中回写 Caffeine）</li>
     *   <li>查数据库（查到的写 Redis + Caffeine，查不到的写空值防穿透）</li>
     * </ol>
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type,
                                           Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;

        // 1. 查 Caffeine L1 本地缓存（纳秒级）
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            return cached == CacheNullValue.INSTANCE ? null : JSONUtil.toBean((String) cached, type);
        }

        // 2. 布隆过滤器拦截：一定不存在的 Key 直接返回
        if (!mightExistByBloom(keyPrefix, id)) {
            return null;
        }

        // 3. 查 Redis L2
        String json = stringRedisTemplate.opsForValue().get(key);

        // 4. 命中且非空 → 回写 Caffeine + 返回
        if (StrUtil.isNotBlank(json)) {
            localCache.put(key, json);
            return JSONUtil.toBean(json, type);
        }

        // 5. 命中空值（防穿透标记）
        if (json != null) {
            localCache.put(key, CacheNullValue.INSTANCE);
            return null;
        }

        // 6. 查数据库
        R r = dbFallback.apply(id);

        // 7. 数据库也不存在 → 写 Redis 空值 + Caffeine 标记
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            localCache.put(key, CacheNullValue.INSTANCE);
            return null;
        }

        // 8. 数据库有值 → 写 Redis + Caffeine（布隆已注册，数据写入时通过管理端完成）
        String valueJson = JSONUtil.toJsonStr(r);
        stringRedisTemplate.opsForValue().set(key, valueJson, time, unit);
        localCache.put(key, valueJson);

        return r;
    }

    /**
     * 缓存穿透防护（全链路多级缓存：Caffeine → 布隆 → 空值 → 互斥锁 → DB）
     * <p>
     * 在 queryWithPassThrough 基础上叠加互斥锁防击穿。
     * 适用场景：热点医院详情、热门医生信息（读并发高 + 重建耗时长）
     */
    public <R, ID> R queryWithMultiLevel(String keyPrefix, ID id, Class<R> type,
                                          Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String lockKey = keyPrefix + "lock:" + id;

        // 1. 查 Caffeine L1
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            return cached == CacheNullValue.INSTANCE ? null : JSONUtil.toBean((String) cached, type);
        }

        // 2. 布隆拦截
        if (!mightExistByBloom(keyPrefix, id)) {
            return null;
        }

        // 3. 查 Redis L2
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            localCache.put(key, json);
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            localCache.put(key, CacheNullValue.INSTANCE);
            return null;
        }

        // 4. Redis miss → 互斥锁 + 查库（其余逻辑对齐 queryWithMutex）
        for (int i = 0; i < MUTEX_MAX_RETRY; i++) {
            if (!tryLock(lockKey)) {
                try {
                    Thread.sleep(MUTEX_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(json)) {
                    localCache.put(key, json);
                    return JSONUtil.toBean(json, type);
                }
                if (json != null) {
                    localCache.put(key, CacheNullValue.INSTANCE);
                    return null;
                }
                continue;
            }
            try {
                // 双重检查
                json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(json)) {
                    localCache.put(key, json);
                    return JSONUtil.toBean(json, type);
                }

                R r = dbFallback.apply(id);
                if (r == null) {
                    stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                    localCache.put(key, CacheNullValue.INSTANCE);
                } else {
                    String valueJson = JSONUtil.toJsonStr(r);
                    stringRedisTemplate.opsForValue().set(key, valueJson, time, unit);
                    localCache.put(key, valueJson);
                }
                return r;
            } finally {
                unlock(lockKey);
            }
        }

        LOGGER.warn("多级缓存获取锁重试超限，降级直查数据库: key={}", key);
        return dbFallback.apply(id);
    }

    /**
     * 布隆过滤包装：根据 keyPrefix 推断实体类型，调用对应过滤器
     */
    private boolean mightExistByBloom(String prefix, Object id) {
        if (prefix == null || id == null) {
            return true; // 无法推断 → 放过
        }
        Long idLong = (id instanceof Long) ? (Long) id : Long.valueOf(id.toString());
        if (prefix.startsWith("cache:info:")) {
            return bloomFilter.mightContainHospital(idLong);
        }
        if (prefix.startsWith("cache:doctor:")) {
            return bloomFilter.mightContainDoctor(idLong);
        }
        if (prefix.startsWith("cache:special:")) {
            return bloomFilter.mightContainSpecial(idLong);
        }
        if (prefix.startsWith("cache:notice:")) {
            return bloomFilter.mightContainNotice(idLong);
        }
        // 未能识别的前缀 → 放过，走常规流程
        return true;
    }

    /**
     * 本地缓存 key 失效（数据变更时同步调用）
     * <p>
     * 注意：此处失效的是带 id 的精确 key；前缀批量失效见 {@link #evictLocalByPrefix(String)}
     */
    public void evictLocalCache(String key) {
        localCache.invalidate(key);
    }

    /**
     * 本地缓存按前缀批量失效（SCAN 遍历 + invalidate）
     */
    public void evictLocalByPrefix(String prefix) {
        // Caffeine 不支持前缀匹配，用迭代器遍历
        localCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * 空值占位对象（Caffeine 中区分"不存在"与"未缓存"）
     */
    private enum CacheNullValue { INSTANCE }

    // ==================== 缓存击穿 — 互斥锁 ====================

    /**
     * 缓存击穿防护（互斥锁重建）
     * <p>
     * 适用场景：热点数据过期时大量请求涌入数据库（热门医生信息）
     *
     * @param keyPrefix  key 前缀
     * @param id         业务 ID
     * @param type       返回类型
     * @param dbFallback 数据库回调
     * @param time       缓存时间
     * @param unit       时间单位
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type,
                                     Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String lockKey = keyPrefix + "lock:" + id;

        // 1. 查 Caffeine L1
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            return cached == CacheNullValue.INSTANCE ? null : JSONUtil.toBean((String) cached, type);
        }

        // 2. 查 Redis L2
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            localCache.put(key, json);
            LOGGER.debug("缓存命中: key={}", key);
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            LOGGER.debug("缓存命中空值: key={}", key);
            return null; // 空值缓存
        }

        LOGGER.debug("缓存未命中，尝试获取互斥锁: key={}", key);

        // 2. 缓存未命中 → 加互斥锁（循环重试有上限，防线程无限空转堆积）
        for (int i = 0; i < MUTEX_MAX_RETRY; i++) {
            if (!tryLock(lockKey)) {
                // 获取锁失败 → 休眠后重试
                try {
                    Thread.sleep(MUTEX_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                // 重试前先查缓存：大概率持锁线程已重建完成，直接返回无需抢锁
                json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(json)) {
                    LOGGER.info("互斥锁重试中，其他线程已重建缓存: key={}", key);
                    return JSONUtil.toBean(json, type);
                }
                if (json != null) {
                    return null; // 空值缓存
                }
                continue;
            }
            try {
                // 3. 双重检查
                json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(json)) {
                    LOGGER.info("双重检查命中缓存: key={}", key);
                    return JSONUtil.toBean(json, type);
                }

                LOGGER.info("获取到互斥锁，查询数据库: key={}", key);

                // 4. 查询数据库
                R r = dbFallback.apply(id);

                // 5. 写 Redis + Caffeine
                if (r == null) {
                    stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                    localCache.put(key, CacheNullValue.INSTANCE);
                } else {
                    String valueJson = JSONUtil.toJsonStr(r);
                    stringRedisTemplate.opsForValue().set(key, valueJson, time, unit);
                    localCache.put(key, valueJson);
                }
                return r;
            } finally {
                unlock(lockKey);
            }
        }

        // 6. 重试超限 → 降级直查数据库（不写缓存），保证可用性
        LOGGER.warn("获取互斥锁重试超限，降级直查数据库: key={}", key);
        return dbFallback.apply(id);
    }

    // ==================== 缓存击穿 — 逻辑过期 ====================

    /**
     * 缓存击穿防护（逻辑过期 + 异步重建）
     * <p>
     * 适用场景：高并发读的热点数据（科室列表）
     *
     * @param keyPrefix  key 前缀
     * @param id         业务 ID
     * @param type       返回类型
     * @param dbFallback 数据库回调
     * @param time       缓存时间
     * @param unit       时间单位
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type,
                                             Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String lockKey = keyPrefix + "lock:" + id;

        // 1. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        // 2. 解析逻辑过期时间
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((String) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 3. 未过期，直接返回
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }

        // 4. 已过期 → 尝试获取锁进行异步重建
        // 注意：锁值含线程标识，必须在当前线程生成 token 并传给异步线程释放，
        // 否则异步线程的锁值不匹配，Lua 校验失败，锁只能等 TTL 自然过期
        String lockToken = getLockValue();
        boolean isLock = tryLock(lockKey, lockToken);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    LOGGER.error("缓存异步重建失败: key={}", key, e);
                } finally {
                    unlock(lockKey, lockToken);
                }
            });
        }

        // 5. 返回旧数据（过期但可用）
        return r;
    }

    // ==================== 分页列表缓存 ====================

    /**
     * 分页列表缓存（互斥锁防击穿 + 空结果防穿透 + TTL 随机抖动防雪崩）
     * <p>
     * 对标黑马点评 queryWithMutex，扩展支持 PageHelper 分页列表：
     * 缓存时把 Page 的分页元数据（pageNum/pageSize/total/pages）一并存入，
     * 命中后还原为 Page 对象，保证 CommonPage.restPage 的总数、页数正确。
     * <p>
     * 适用场景：读多写少的分页列表（医生列表、排班搜索、医院列表热点页）
     *
     * @param key         完整缓存 key（含查询参数）
     * @param elementType 列表元素类型
     * @param dbFallback  数据库回调（内部执行 PageHelper.startPage + 查询）
     * @param time        基础缓存时间（实际写入时叠加随机抖动）
     * @param unit        时间单位
     * @param <R>         元素类型泛型
     * @return 列表（命中缓存时为还原的 Page 对象；空结果返回空列表）
     */
    public <R> List<R> queryPageWithMutex(String key, Class<R> elementType,
                                          Supplier<List<R>> dbFallback, Long time, TimeUnit unit) {
        // 1. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            LOGGER.debug("分页缓存命中: key={}", key);
            return parsePageJson(json, elementType);
        }

        LOGGER.debug("分页缓存未命中，尝试获取互斥锁: key={}", key);

        // 2. 缓存未命中 → 加互斥锁重建（循环重试有上限，防线程无限空转堆积）
        String lockKey = key + ":lock";
        for (int i = 0; i < MUTEX_MAX_RETRY; i++) {
            if (!tryLock(lockKey)) {
                // 获取锁失败 → 休眠后重试
                try {
                    Thread.sleep(MUTEX_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new ArrayList<>();
                }
                // 重试前先查缓存：大概率持锁线程已重建完成，直接返回无需抢锁
                json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(json)) {
                    LOGGER.debug("互斥锁重试中，其他线程已重建分页缓存: key={}", key);
                    return parsePageJson(json, elementType);
                }
                continue;
            }
            try {
                // 3. 双重检查
                json = stringRedisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(json)) {
                    return parsePageJson(json, elementType);
                }

                LOGGER.debug("获取到互斥锁，查询数据库重建分页缓存: key={}", key);

                // 4. 查询数据库
                List<R> list = dbFallback.get();

                // 5. 写缓存：空结果短 TTL 防穿透；正常结果 TTL 加随机抖动防雪崩
                if (list == null || list.isEmpty()) {
                    LOGGER.debug("分页结果为空，写入空值缓存: key={}", key);
                    stringRedisTemplate.opsForValue()
                            .set(key, toPageJson(list), CACHE_NULL_TTL, TimeUnit.MINUTES);
                    return list == null ? new ArrayList<>() : list;
                }
                LOGGER.debug("写入分页缓存: key={}", key);
                stringRedisTemplate.opsForValue()
                        .set(key, toPageJson(list), randomTtlSeconds(time, unit), TimeUnit.SECONDS);
                return list;
            } finally {
                unlock(lockKey);
            }
        }

        // 6. 重试超限 → 降级直查数据库（不写缓存），保证可用性
        LOGGER.warn("获取互斥锁重试超限，降级直查数据库: key={}", key);
        List<R> list = dbFallback.get();
        return list == null ? new ArrayList<>() : list;
    }

    /**
     * 分页列表缓存（逻辑过期 + 异步重建防击穿）
     * <p>
     * 对标黑马点评 queryWithLogicalExpire，两点改进：
     * 1. 支持 PageHelper 分页列表（同 queryPageWithMutex）；
     * 2. 缓存未命中时同步加载并写入（自预热），而非直接返回 null，
     *    解决黑马原版必须提前手动预热、漏预热即查不到数据的缺陷。
     * <p>
     * 适用场景：几乎不变的高频热点列表（专科列表、门诊列表）
     *
     * @param key         完整缓存 key（含查询参数）
     * @param elementType 列表元素类型
     * @param dbFallback  数据库回调
     * @param time        逻辑过期时间
     * @param unit        时间单位
     * @param <R>         元素类型泛型
     * @return 列表（逻辑过期时返回旧数据，由异步线程重建）
     */
    public <R> List<R> queryPageWithLogicalExpire(String key, Class<R> elementType,
                                                  Supplier<List<R>> dbFallback, Long time, TimeUnit unit) {
        // 1. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2. 未命中 → 同步加载并写入逻辑过期缓存（自预热）
        if (StrUtil.isBlank(json)) {
            LOGGER.info("逻辑过期缓存未命中，自预热加载: key={}", key);
            List<R> list = dbFallback.get();
            setPageWithLogicalExpire(key, list, time, unit);
            return list == null ? new ArrayList<>() : list;
        }

        // 3. 命中 → 解析逻辑过期时间
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        List<R> list = parsePageJson((String) redisData.getData(), elementType);

        // 4. 未过期，直接返回
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            LOGGER.debug("逻辑过期缓存未过期: key={}", key);
            return list;
        }

        LOGGER.debug("逻辑过期缓存已过期，触发热点重建: key={}", key);

        // 5. 已过期 → 获取锁成功者异步重建（锁 token 在当前线程生成，交由异步线程释放）
        String lockKey = key + ":lock";
        String lockToken = getLockValue();
        if (tryLock(lockKey, lockToken)) {
            LOGGER.info("获得逻辑过期重建锁，提交异步重建任务: key={}", key);
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    setPageWithLogicalExpire(key, dbFallback.get(), time, unit);
                    LOGGER.info("异步重建完成，缓存已更新: key={}", key);
                } catch (Exception e) {
                    LOGGER.error("分页列表缓存异步重建失败: key={}", key, e);
                } finally {
                    unlock(lockKey, lockToken);
                }
            });
        }

        // 6. 返回旧数据（过期但可用）
        return list;
    }

    /**
     * 写入带逻辑过期时间的分页列表缓存（预热/重建共用）
     */
    public <R> void setPageWithLogicalExpire(String key, List<R> list, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(toPageJson(list));
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 按前缀删除缓存（Redis SCAN + Caffeine 同步失效）
     */
    public void deleteByPrefix(String prefix) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(100)
                .build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        // 同步失效 Caffeine L1（前缀匹配删除）
        evictLocalByPrefix(prefix);
    }

    /**
     * 序列化分页列表：PageHelper 的 Page 对象额外保存分页元数据
     */
    private <R> String toPageJson(List<R> list) {
        JSONObject obj = JSONUtil.createObj();
        if (list instanceof Page) {
            Page<R> page = (Page<R>) list;
            obj.set("paged", true)
                    .set("pageNum", page.getPageNum())
                    .set("pageSize", page.getPageSize())
                    .set("total", page.getTotal())
                    .set("pages", page.getPages());
        } else {
            obj.set("paged", false);
        }
        obj.set("list", list == null ? new ArrayList<>() : list);
        return obj.toString();
    }

    /**
     * 反序列化分页列表：带分页元数据的还原为 Page 对象，保证分页信息不丢失
     */
    private <R> List<R> parsePageJson(String json, Class<R> elementType) {
        JSONObject obj = JSONUtil.parseObj(json);
        List<R> items = JSONUtil.toList(obj.getJSONArray("list"), elementType);
        if (Boolean.TRUE.equals(obj.getBool("paged", false))) {
            Page<R> page = new Page<>(obj.getInt("pageNum", 1), obj.getInt("pageSize", items.size()));
            page.setTotal(obj.getLong("total", (long) items.size()));
            page.setPages(obj.getInt("pages", 1));
            page.addAll(items);
            return page;
        }
        return items;
    }

    /**
     * TTL 随机抖动（防雪崩）：基础时间上叠加 0~20% 的随机偏移
     */
    private long randomTtlSeconds(Long time, TimeUnit unit) {
        long baseSeconds = unit.toSeconds(time);
        return baseSeconds + RANDOM.nextInt((int) Math.max(1, baseSeconds / 5));
    }

    // ==================== 辅助方法 ====================

    /**
     * 设置带逻辑过期时间的缓存
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(JSONUtil.toJsonStr(value));
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 普通缓存写入（Redis + Caffeine 双写）
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        String json = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key, json, time, unit);
        localCache.put(key, json);
    }

    /**
     * 锁的值前缀（UUID + 线程ID，确保线程安全释放）
     */
    private static final String LOCK_VALUE_PREFIX = UUID.randomUUID().toString().replace("-", "") + "-";

    /**
     * 释放锁的 Lua 脚本（原子判断+删除，防止误删他人的锁）
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end"
        );
    }

    /**
     * 获取当前线程的锁标识
     */
    private String getLockValue() {
        return LOCK_VALUE_PREFIX + Thread.currentThread().getId();
    }

    /**
     * SETNX 获取互斥锁（带唯一线程标识）
     */
    private boolean tryLock(String key) {
        return tryLock(key, getLockValue());
    }

    /**
     * SETNX 获取互斥锁（显式指定锁 token，用于跨线程释放场景）
     */
    private boolean tryLock(String key, String token) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁（Lua原子校验：只有锁值匹配才删除）
     */
    private void unlock(String key) {
        unlock(key, getLockValue());
    }

    /**
     * 释放互斥锁（显式指定锁 token，用于跨线程释放场景）
     */
    private void unlock(String key, String token) {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),
                token
        );
    }

    // ==================== 内部类 ====================

    /**
     * Redis 逻辑过期数据结构
     */
    static class RedisData {
        private LocalDateTime expireTime;
        private Object data;

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
