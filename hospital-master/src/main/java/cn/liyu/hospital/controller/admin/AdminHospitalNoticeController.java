package cn.liyu.hospital.controller.admin;

import cn.liyu.hospital.common.api.CommonResult;
import cn.liyu.hospital.entity.HospitalNotice;
import cn.liyu.hospital.mapper.HospitalNoticeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Set;

/**
 * 管理端 · 健康资讯发布 — 医院官号发布资讯，推送到关注者 Feed 收件箱
 *
 * @author 医秒通
 */
@Tag(name = "管理端 · 健康资讯", description = "医院发布健康资讯")
@RestController
@RequestMapping("/notice")
public class AdminHospitalNoticeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminHospitalNoticeController.class);

    private static final String FOLLOW_KEY = "follow:hospital:";
    private static final String FEED_KEY = "feed:user:";

    /**
     * 资讯列表缓存前缀（与 HospitalQueryController 查询侧一致，发布后删除）
     */
    private static final String CACHE_NOTICE_LIST_KEY = "cache:notice:list:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HospitalNoticeMapper noticeMapper;

    @Operation(summary = "发布健康资讯", description = "医院官号发布资讯，推送到所有关注者Feed。图片先通过 /picture/upload 上传获取URL")
    @PostMapping
    public CommonResult<String> publishNotice(@RequestParam Long hospitalId,
                                       @RequestParam String title,
                                       @RequestParam String content,
                                       @RequestParam(required = false) String picture) {
        HospitalNotice notice = new HospitalNotice();
        notice.setHospitalId(hospitalId);
        notice.setTitle(title);
        notice.setContent(content);
        if(picture != null && !picture.isEmpty()) notice.setPicture(picture);
        notice.setGmtCreate(new Date());
        notice.setGmtModified(new Date());
        noticeMapper.insert(notice);

        // 先写数据库，再删除资讯列表缓存（该医院的列表 + 全部资讯列表）
        stringRedisTemplate.delete(CACHE_NOTICE_LIST_KEY + hospitalId);
        stringRedisTemplate.delete(CACHE_NOTICE_LIST_KEY + "all");

        String feedValue = "notice:" + notice.getId() + ":" + hospitalId;
        String followKey = FOLLOW_KEY + hospitalId;
        Set<String> followers = stringRedisTemplate.opsForSet().members(followKey);

        if (followers == null || followers.isEmpty()) {
            LOGGER.warn("医院无关注者，资讯仅记录: hospitalId={}, noticeId={}", hospitalId, notice.getId());
            return CommonResult.success("资讯发布成功（当前无关注者）");
        }

        long pushTime = System.currentTimeMillis();
        for (String followerId : followers) {
            stringRedisTemplate.opsForZSet().add(FEED_KEY + followerId, feedValue, pushTime);
        }

        LOGGER.info("资讯推送完成: noticeId={}, hospitalId={}, 推送给{}位关注者", notice.getId(), hospitalId, followers.size());
        return CommonResult.success("资讯发布成功，已推送给 " + followers.size() + " 位关注者");
    }
}
