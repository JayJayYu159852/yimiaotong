package cn.liyu.hospital.controller.user;

import cn.hutool.core.collection.CollUtil;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.common.security.UserHolder;
import cn.liyu.hospital.dto.FeedItemDTO;
import cn.liyu.hospital.dto.ScrollResult;
import cn.liyu.hospital.entity.HospitalFollow;
import cn.liyu.hospital.entity.HospitalNotice;
import cn.liyu.hospital.mapper.HospitalFollowMapper;
import cn.liyu.hospital.mapper.HospitalNoticeMapper;
import cn.liyu.hospital.service.IHospitalInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 医院关注/Feed流控制器（对标黑马点评 FollowController + BlogController）
 * <p>
 * 关注采用 DB + Redis 双写：
 * - DB：hospital_follow 表持久化
 * - Redis Set：follow:hospital:{hospitalId} → userIds（医院粉丝）
 * - Redis Set：follow:user:{userId} → hospitalIds（用户关注了哪些医院）
 *
 * @author 医秒通
 */
@Tag(name = "用户端 · 关注Feed", description = "医院关注与健康资讯Feed流")
@RestController
@RequestMapping("/follow")
public class HospitalFollowController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HospitalFollowController.class);

    private static final String FOLLOW_KEY = "follow:hospital:";
    private static final String USER_FOLLOW_KEY = "follow:user:";
    private static final String FEED_KEY = "feed:user:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IHospitalInfoService hospitalInfoService;

    @Resource
    private HospitalFollowMapper followMapper;

    @Resource
    private HospitalNoticeMapper noticeMapper;

    /**
     * 关注/取关医院（DB + Redis 双写，对标黑马点评）
     */
    @Operation(summary = "关注/取关医院")
    @PutMapping("/{hospitalId}/{isFollow}")
    public CommonResult<String> follow(@PathVariable Long hospitalId,
                                @PathVariable Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + hospitalId;

        if (isFollow) {
            // 1. DB 持久化
            HospitalFollow follow = new HospitalFollow();
            follow.setUserId(userId);
            follow.setHospitalId(hospitalId);
            follow.setGmtCreate(new Date());
            try {
                followMapper.insert(follow);
            } catch (Exception e) {
                return CommonResult.failed("您已关注该医院");
            }

            // 2. Redis 双向维护
            stringRedisTemplate.opsForSet().add(key, String.valueOf(userId));
            stringRedisTemplate.opsForSet().add(USER_FOLLOW_KEY + userId, String.valueOf(hospitalId));
            LOGGER.info("关注成功: userId={}, hospitalId={}", userId, hospitalId);
        } else {
            // 1. DB 删除
            followMapper.deleteByUserIdAndHospitalId(userId, hospitalId);

            // 2. Redis 双向删除
            stringRedisTemplate.opsForSet().remove(key, String.valueOf(userId));
            stringRedisTemplate.opsForSet().remove(USER_FOLLOW_KEY + userId, String.valueOf(hospitalId));
            LOGGER.info("取关成功: userId={}, hospitalId={}", userId, hospitalId);
        }

        return CommonResult.success(isFollow ? "关注成功" : "已取消关注");
    }

    /**
     * 判断是否已关注（优先 Redis，对标黑马点评）
     */
    @Operation(summary = "判断是否已关注")
    @GetMapping("/or/not/{hospitalId}")
    public CommonResult<Boolean> isFollow(@PathVariable Long hospitalId) {
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOW_KEY + hospitalId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, String.valueOf(userId));
        if (Boolean.TRUE.equals(isMember)) {
            return CommonResult.success(true);
        }
        // Redis 未命中，查 DB 兜底
        int count = followMapper.countByUserIdAndHospitalId(userId, hospitalId);
        return CommonResult.success(count > 0);
    }

    /**
     * 获取共同关注（Redis Set 交集，对标黑马点评）
     */
    @Operation(summary = "获取共同关注")
    @GetMapping("/common/{otherUserId}")
    public CommonResult<List<Long>> followCommons(@PathVariable Long otherUserId) {
        Long userId = UserHolder.getUser().getId();
        String key1 = USER_FOLLOW_KEY + userId;
        String key2 = USER_FOLLOW_KEY + otherUserId;

        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);

        if (CollUtil.isEmpty(intersect)) {
            return CommonResult.success(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>();
        for (String s : intersect) {
            ids.add(Long.valueOf(s));
        }
        return CommonResult.success(ids);
    }

    /**
     * Feed流 — 滚动分页查询（SortedSet，对标黑马点评）
     */
    /**
     * Feed流 — 滚动分页查询（对标黑马点评 queryBlogOfFollow）
     * <p>
     * 流程：SortedSet 取 ID → 批量查 DB 补全内容 → 按原序返回
     */
    @Operation(summary = "获取Feed流（滚动分页）")
    @GetMapping("/feed")
    public CommonResult<ScrollResult> queryFeed(@RequestParam(defaultValue = "9223372036854775807") Long lastId,
                                                 @RequestParam(defaultValue = "0") Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key = FEED_KEY + userId;

        // 1. ZREVRANGEBYSCORE key Max Min LIMIT offset count（倒序，最新在前）
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet()
                        .reverseRangeByScoreWithScores(key, 0, lastId, offset, 5);

        if (CollUtil.isEmpty(typedTuples)) {
            ScrollResult empty = new ScrollResult();
            empty.setList(Collections.emptyList());
            empty.setMinTime(0L);
            empty.setOffset(0);
            return CommonResult.success(empty);
        }

        // 2. 解析数据：noticeId、minTime、offset（对标黑马点评）
        List<Long> noticeIds = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;

        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            String value = tuple.getValue();  // 格式: "notice:{id}:{hospitalId}"
            long time = tuple.getScore() != null ? tuple.getScore().longValue() : 0;

            // 提取 noticeId
            String[] parts = value.split(":");
            if (parts.length >= 2) {
                noticeIds.add(Long.valueOf(parts[1]));
            }

            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }

        // 3. 批量查询 DB，按 SortedSet 原序组装（对标 MySQL FIELD 函数排序）
        Map<Long, HospitalNotice> noticeMap = new LinkedHashMap<>();
        for (Long nid : noticeIds) {
            HospitalNotice notice = noticeMapper.selectById(nid);
            if (notice != null) {
                noticeMap.put(nid, notice);
            }
        }

        List<FeedItemDTO> feedList = noticeIds.stream()
                .filter(noticeMap::containsKey)
                .map(nid -> {
                    HospitalNotice notice = noticeMap.get(nid);
                    return FeedItemDTO.builder()
                            .noticeId(notice.getId())
                            .hospitalId(notice.getHospitalId())
                            .hospitalName(hospitalInfoService.getName(notice.getHospitalId()))
                            .title(notice.getTitle())
                            .content(notice.getContent().length() > 100
                                    ? notice.getContent().substring(0, 100) + "..." : notice.getContent())
                            .build();
                })
                .collect(Collectors.toList());

        // 4. 滚动分页封装并返回
        ScrollResult result = new ScrollResult();
        result.setList(feedList);
        result.setMinTime(minTime);
        result.setOffset(os);
        return CommonResult.success(result);
    }

    /**
     * 查询关注医院的全部历史资讯（DB直查，按时间倒序）
     * <p>
     * 与 Feed 流（Redis SortedSet 推送）互补：Feed 流存放新发布推送，
     * 本接口让用户像刷朋友圈一样翻阅所有关注医院的历史资讯。
     */
    @Operation(summary = "获取关注医院的全部历史资讯")
    @GetMapping("/notices")
    public CommonResult<List<HospitalNotice>> queryFollowedNotices() {
        Long userId = UserHolder.getUser().getId();

        // 1. 查出用户关注的所有医院 ID（Redis Set → DB 兜底）
        Set<String> hospitalIdStrs = stringRedisTemplate.opsForSet()
                .members(USER_FOLLOW_KEY + userId);

        List<Long> hospitalIds;
        if (CollUtil.isEmpty(hospitalIdStrs)) {
            // Redis 为空，查 DB
            hospitalIds = followMapper.selectHospitalIdsByUserId(userId);
            if (CollUtil.isEmpty(hospitalIds)) {
                return CommonResult.success(Collections.emptyList());
            }
        } else {
            hospitalIds = hospitalIdStrs.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }

        // 2. 批量查询这些医院的全部资讯
        List<HospitalNotice> notices = noticeMapper.selectByHospitalIds(hospitalIds);
        return CommonResult.success(notices != null ? notices : Collections.emptyList());
    }
}
