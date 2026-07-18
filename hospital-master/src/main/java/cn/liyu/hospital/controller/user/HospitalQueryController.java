package cn.liyu.hospital.controller.user;

import cn.liyu.hospital.common.api.CommonPage;
import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.component.CacheClient;
import cn.liyu.hospital.component.UvStatisticsComponent;
import cn.liyu.hospital.dto.DoctorRankDTO;
import cn.liyu.hospital.dto.HospitalDoctorDTO;
import cn.liyu.hospital.dto.HospitalNearbyDTO;
import cn.liyu.hospital.entity.*;
import cn.liyu.hospital.mapper.HospitalNoticeMapper;
import cn.liyu.hospital.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户端 · 首页浏览 — 医院→专科→医生 级联查询（全只读）
 *
 * @author 医秒通
 */
@Tag(name = "用户端 · 首页浏览", description = "医院/专科/门诊/医生查询")
@RestController
@RequestMapping("/hospital")
public class HospitalQueryController {

    private static final String GEO_KEY = "hospital:geo";
    private static final String RANK_SCORE_KEY = "doctor:rank:score";
    private static final String RANK_COUNT_KEY = "doctor:rank:count";

    /**
     * 资讯列表缓存前缀（互斥锁 + 30分钟 TTL，发布资讯时删除）
     */
    private static final String CACHE_NOTICE_LIST_KEY = "cache:notice:list:";

    /**
     * 资讯列表缓存时间（30分钟，资讯低频更新）
     */
    private static final long CACHE_NOTICE_LIST_TTL = 30L;

    @Resource
    private IHospitalInfoService infoService;
    @Resource
    private IHospitalSpecialService specialService;
    @Resource
    private IHospitalDoctorService doctorService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UvStatisticsComponent uvStatisticsComponent;
    @Resource
    private CacheClient cacheClient;

    @Resource
    private HospitalNoticeMapper noticeMapper;

    // ==================== 医院 ====================

    @Operation(summary = "首页：医院列表")
    @GetMapping("/info/list")
    public CommonResult<CommonPage<HospitalInfo>> listHospitals(@RequestParam(required = false) String name,
                                                                 @RequestParam Integer pageNum,
                                                                 @RequestParam Integer pageSize) {
        List<HospitalInfo> l = infoService.list(name, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(l != null ? l : Collections.emptyList()));
    }

    @Operation(summary = "医院详情（含UV统计）")
    @GetMapping("/info/{id}")
    public CommonResult<HospitalInfo> getHospital(@PathVariable Long id,
                                                   @RequestParam(required = false) Long userId) {
        // 不做前置 count 检查，直接走缓存链路：不存在的 id 由空值缓存拦截（防穿透）
        return infoService.getOptional(id)
                .map(info -> {
                    if (userId != null) {
                        uvStatisticsComponent.recordHospitalUV(id, userId);
                    }
                    return CommonResult.success(info);
                })
                .orElse(CommonResult.validateFailed("不存在，该医院编号！"));
    }

    @Operation(summary = "附近医院", description = "传入经度、纬度，默认5000km（不限制距离）")
    @GetMapping("/info/nearby")
    public CommonResult<List<HospitalNearbyDTO>> searchNearby(@RequestParam Double lng,
                                                               @RequestParam Double lat,
                                                               @RequestParam(defaultValue = "5000") Double radius) {
        Distance distance = new Distance(radius, Metrics.KILOMETERS);
        Circle circle = new Circle(new Point(lng, lat), distance);
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs().includeDistance().limit(20);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(GEO_KEY, circle, args);

        if (results == null) return CommonResult.success(Collections.emptyList());

        List<HospitalNearbyDTO> list = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : results) {
            String hospitalId = geoResult.getContent().getName();
            double distKm = geoResult.getDistance().getValue();
            infoService.getOptional(Long.valueOf(hospitalId)).ifPresent(hospital ->
                list.add(HospitalNearbyDTO.builder()
                        .hospitalId(hospital.getId()).hospitalName(hospital.getName())
                        .address(hospital.getAddress()).phone(hospital.getPhone())
                        .distance(Math.round(distKm * 100.0) / 100.0).build()));
        }
        return CommonResult.success(list);
    }

    // ==================== 专科 ====================

    @Operation(summary = "专科列表")
    @GetMapping("/special/list")
    public CommonResult<CommonPage<HospitalSpecial>> listSpecials(@RequestParam(required = false) String name,
                                                                   @RequestParam Integer pageNum,
                                                                   @RequestParam Integer pageSize) {
        List<HospitalSpecial> l1 = specialService.list(name, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(l1 != null ? l1 : Collections.emptyList()));
    }

    @Operation(summary = "医院下有哪些专科")
    @GetMapping("/special/list/{hospitalId}")
    public CommonResult<CommonPage<HospitalSpecial>> listSpecialByHospital(@PathVariable Long hospitalId,
                                                                           @RequestParam Integer pageNum,
                                                                           @RequestParam Integer pageSize) {
        if (!infoService.count(hospitalId)) {
            return CommonResult.validateFailed("不存在，该医院编号！");
        }
        List<HospitalSpecial> l2 = specialService.list(hospitalId, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(l2 != null ? l2 : Collections.emptyList()));
    }

    // ==================== 医生 ====================

    @Operation(summary = "医生列表")
    @GetMapping("/doctor/list")
    public CommonResult<CommonPage<HospitalDoctorDTO>> listDoctors(@RequestParam(required = false) String name,
                                                                    @RequestParam Integer pageNum,
                                                                    @RequestParam Integer pageSize) {
        List<HospitalDoctorDTO> l5 = doctorService.list(name, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(l5 != null ? l5 : Collections.emptyList()));
    }

    @Operation(summary = "按专科找医生")
    @GetMapping("/doctor/list/special/outpatient")
    public CommonResult<CommonPage<HospitalDoctorDTO>> listDoctorsBySpecial(@RequestParam Long specialId,
                                                                             @RequestParam Integer pageNum,
                                                                             @RequestParam Integer pageSize) {
        List<HospitalDoctorDTO> l6 = doctorService.list(null, specialId, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(l6 != null ? l6 : Collections.emptyList()));
    }

    @Operation(summary = "医生详情（含UV统计）")
    @GetMapping("/doctor/{id}")
    public CommonResult<HospitalDoctorDTO> getDoctor(@PathVariable Long id,
                                                      @RequestParam(required = false) Long userId) {
        if (!doctorService.count(id)) {
            return CommonResult.validateFailed("不存在，该医生编号");
        }
        if (userId != null) {
            uvStatisticsComponent.recordDoctorUV(id, userId);
        }
        return doctorService.getConvert(id)
                .map(CommonResult::success)
                .orElse(CommonResult.failed("服务器错误！"));
    }

    @Operation(summary = "医生好评排行榜 Top10")
    @GetMapping("/doctor/rank/top10")
    public CommonResult<List<DoctorRankDTO>> top10Rank() {
        Set<ZSetOperations.TypedTuple<String>> topSet =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(RANK_SCORE_KEY, 0, 9);
        if (topSet == null || topSet.isEmpty()) return CommonResult.success(Collections.emptyList());

        List<DoctorRankDTO> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : topSet) {
            Long doctorId = Long.valueOf(tuple.getValue());
            Double score = tuple.getScore();
            Double count = stringRedisTemplate.opsForZSet().score(RANK_COUNT_KEY, String.valueOf(doctorId));
            doctorService.getOptional(doctorId).ifPresent(doctor ->
                list.add(DoctorRankDTO.builder()
                        .doctorId(doctorId).doctorName(doctor.getName())
                        .jobTitle(doctor.getJobTitle()).specialName(specialService.getName(doctor.getSpecialId()))
                        .score(score != null ? Math.round(score * 100.0) / 100.0 : 0.0)
                        .rateCount(count != null ? count.longValue() : 0L).build()));
        }
        return CommonResult.success(list);
    }

    // ==================== 资讯 ====================

    @Operation(summary = "医院资讯列表")
    @GetMapping("/notice/list")
    public CommonResult<List<HospitalNotice>> listNotices(@RequestParam Long hospitalId) {
        // 资讯低频更新 → 互斥锁缓存 + 发布时删除（对标黑马点评列表缓存练习）
        return CommonResult.success(cacheClient.queryPageWithMutex(
                CACHE_NOTICE_LIST_KEY + hospitalId, HospitalNotice.class,
                () -> noticeMapper.selectByHospitalId(hospitalId),
                CACHE_NOTICE_LIST_TTL, TimeUnit.MINUTES));
    }

    @Operation(summary = "全部医院资讯（按时间倒序）")
    @GetMapping("/notice/list/all")
    public CommonResult<List<HospitalNotice>> listAllNotices() {
        return CommonResult.success(cacheClient.queryPageWithMutex(
                CACHE_NOTICE_LIST_KEY + "all", HospitalNotice.class,
                () -> noticeMapper.selectAllOrderByTime(),
                CACHE_NOTICE_LIST_TTL, TimeUnit.MINUTES));
    }

}
