package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 患者健康打卡签到控制器（BitMap）
 *
 * @author 医秒通
 * @date 2024/7/13
 */
@Tag(name = "用户端 · 健康打卡", description = "每日健康打卡签到（Redis BitMap）")
@RestController
@RequestMapping("/user")
public class UserSignController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSignController.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "每日签到", description = "传入用户编号")
    @PostMapping("/sign")
    public CommonResult<String> sign(@RequestParam Long userId) {
        LocalDate now = LocalDate.now();
        String key = "sign:" + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOfMonth = now.getDayOfMonth();

        // SETBIT key offset value
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);

        LOGGER.info("签到成功: userId={}, date={}", userId, now);
        return CommonResult.success("签到成功！");
    }

    @Operation(summary = "当月签到天数及每日日历", description = "传入用户编号，返回签到天数+今日是否签到+每日位图")
    @GetMapping("/sign/count")
    public CommonResult<Map<String, Object>> signCount(@RequestParam Long userId) {
        LocalDate now = LocalDate.now();
        String key = "sign:" + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int daysInMonth = now.lengthOfMonth();

        // BITCOUNT 统计当月签到天数
        Long count = stringRedisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.bitCount(key.getBytes(StandardCharsets.UTF_8))
        );

        // 构建每日签到位图（供前端绘制日历）
        List<Boolean> signBitmap = new ArrayList<>(daysInMonth);
        for (int i = 0; i < daysInMonth; i++) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i);
            signBitmap.add(Boolean.TRUE.equals(bit));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("month", now.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        result.put("signDays", count != null ? count : 0);
        result.put("signed", isSignedToday(key, now.getDayOfMonth()));
        result.put("signBitmap", signBitmap);
        result.put("daysInMonth", daysInMonth);

        return CommonResult.success(result);
    }

    @Operation(summary = "连续签到天数", description = "传入用户编号")
    @GetMapping("/sign/continuous")
    public CommonResult<Map<String, Object>> signContinuous(@RequestParam Long userId) {
        LocalDate now = LocalDate.now();
        String key = "sign:" + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int dayOfMonth = now.getDayOfMonth();

        // 从今天开始倒着数，逐一位检查是否签到
        int continuousDays = 0;
        for (int i = dayOfMonth - 1; i >= 0; i--) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i);
            if (Boolean.TRUE.equals(bit)) {
                continuousDays++;
            } else {
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("continuousDays", continuousDays);
        return CommonResult.success(result);
    }

    private boolean isSignedToday(String key, int dayOfMonth) {
        Boolean bit = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
        return Boolean.TRUE.equals(bit);
    }
}
