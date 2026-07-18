package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.component.CacheClient;
import cn.liyu.hospital.dto.param.HospitalInfoParam;
import cn.liyu.hospital.dto.param.HospitalSpecialRelationParam;
import org.springframework.data.redis.core.StringRedisTemplate;
import cn.liyu.hospital.entity.*;
import cn.liyu.hospital.mapper.HospitalInfoMapper;
import cn.liyu.hospital.mapper.HospitalSpecialRelationMapper;
import cn.liyu.hospital.service.IHospitalInfoService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author 医秒通
 */

@Service
public class
HospitalInfoServiceImpl implements IHospitalInfoService {

    @Resource
    private HospitalInfoMapper infoMapper;

    @Resource
    private HospitalSpecialRelationMapper specialRelationMapper;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 医院详情缓存前缀（统一 cache: 命名规范，与 cache:doctor: / cache:special: 等保持一致）
     */
    private static final String CACHE_HOSPITAL_KEY = "cache:hospital:";

    /**
     * 医院列表热点页缓存前缀（互斥锁 + TTL 方案，写操作时按前缀删除）
     */
    private static final String CACHE_HOSPITAL_LIST_KEY = "cache:hospital:list:";

    /**
     * 专科列表（按医院）缓存前缀（医院-专科关系变更时按前缀删除）
     */
    private static final String CACHE_SPECIAL_LIST_HOSPITAL_KEY = "cache:special:list:hospital:";

    /**
     * 列表缓存时间（10分钟）
     */
    private static final long CACHE_LIST_TTL = 10L;

    /**
     * 首页热点页范围：仅缓存无搜索词的前 3 页（首页入口流量集中在前几页）
     */
    private static final int HOT_PAGE_LIMIT = 3;

    private static final Random RANDOM = new Random();

    /**
     * 添加医院信息
     *
     * @param param 医院信息参数
     * @return 新生成的医院编号（失败返回 null）
     */
    @Override
    public Long insert(HospitalInfoParam param) {
        HospitalInfo info = new HospitalInfo();

        BeanUtils.copyProperties(param, info);

        info.setGmtCreate(new Date());
        info.setGmtModified(new Date());

        int rows = infoMapper.insertSelective(info);
        if (rows > 0) {
            // 新增医院 → 列表热点页缓存失效
            cacheClient.deleteByPrefix(CACHE_HOSPITAL_LIST_KEY);
            // Mapper 未配置 useGeneratedKeys，通过唯一手机号反查生成的 ID
            HospitalInfoExample example = new HospitalInfoExample();
            example.createCriteria().andPhoneEqualTo(param.getPhone());
            List<HospitalInfo> list = infoMapper.selectByExample(example);
            if (!list.isEmpty()) {
                return list.get(0).getId();
            }
        }
        return null;
    }

    /**
     * 更新医院信息
     *
     * @param id    医院编号
     * @param param 医院信息参数
     * @return 是否成功
     */
    @Override
    public boolean update(Long id, HospitalInfoParam param) {
        HospitalInfo info = new HospitalInfo();
        BeanUtils.copyProperties(param, info);
        info.setId(id);
        info.setGmtModified(new Date());
        if (infoMapper.updateByPrimaryKeySelective(info) > 0) {
            // 更新后删除缓存，下次查询重新加载：详情缓存 + 列表热点页缓存
            stringRedisTemplate.delete(CACHE_HOSPITAL_KEY + id);
            cacheClient.deleteByPrefix(CACHE_HOSPITAL_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 获取医院名称
     */
    @Override
    public String getName(Long id) {
        return getOptional(id).map(HospitalInfo::getName).orElse("未知");
    }

    /**
     * 获取医院信息（缓存穿透防护：空值缓存 + 30分钟TTL）
     */
    @Override
    public Optional<HospitalInfo> getOptional(Long id) {
        HospitalInfo info = cacheClient.queryWithMutex(
                CACHE_HOSPITAL_KEY, id, HospitalInfo.class,
                infoMapper::selectByPrimaryKey, 30L + RANDOM.nextInt(10), TimeUnit.MINUTES
        );
        return Optional.ofNullable(info);
    }

    /**
     * 删除医院信息
     */
    @Override
    public boolean delete(Long id) {
        if (infoMapper.deleteByPrimaryKey(id) > 0) {
            stringRedisTemplate.delete(CACHE_HOSPITAL_KEY + id);
            cacheClient.deleteByPrefix(CACHE_HOSPITAL_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 判断电话是否存在
     *
     * @param phone 电话
     * @return 是否存在
     */
    @Override
    public boolean count(String phone) {
        HospitalInfoExample example = new HospitalInfoExample();

        example.createCriteria()
                .andPhoneEqualTo(phone);

        return infoMapper.countByExample(example) > 0;
    }

    /**
     * 判断医院信息是否存在
     *
     * @param id 医院编号
     * @return 是否存在
     */
    @Override
    public boolean count(Long id) {
        HospitalInfoExample example = new HospitalInfoExample();

        example.createCriteria()
                .andIdEqualTo(id);

        return infoMapper.countByExample(example) > 0;
    }

    /**
     * 查找医院列表
     * <p>
     * 首页入口流量最大 → 仅缓存无搜索词的前 3 页热点页（互斥锁 + 10分钟 TTL + 写时删除）；
     * 带名称搜索或深分页直查数据库
     *
     * @param name     医院名称
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 医院列表
     */
    @Override
    public List<HospitalInfo> list(String name, Integer pageNum, Integer pageSize) {

        if (StrUtil.isNotEmpty(name) || pageNum == null || pageNum > HOT_PAGE_LIMIT) {
            return selectByName(name, pageNum, pageSize);
        }

        return cacheClient.queryPageWithMutex(
                CACHE_HOSPITAL_LIST_KEY + pageNum + ":" + pageSize, HospitalInfo.class,
                () -> selectByName(null, pageNum, pageSize),
                CACHE_LIST_TTL, TimeUnit.MINUTES);
    }

    /**
     * 数据库查询：按名称模糊查找医院列表
     */
    private List<HospitalInfo> selectByName(String name, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        HospitalInfoExample example = new HospitalInfoExample();

        if (StrUtil.isNotEmpty(name)) {
            example.createCriteria()
                    .andNameLike("%" + name + "%");

        }

        return infoMapper.selectByExample(example);
    }

    /**
     * 插入专科到医院中去
     *
     * @param param 医院专科关系参数
     * @return 是否成功
     */
    @Override
    public boolean insertSpecialRelation(HospitalSpecialRelationParam param) {
        HospitalSpecialRelation relation = new HospitalSpecialRelation();

        BeanUtils.copyProperties(param, relation);

        relation.setGmtCreate(new Date());
        relation.setGmtModified(new Date());

        if (specialRelationMapper.insertSelective(relation) > 0) {
            // 医院-专科关系变更 → "医院下的专科"列表缓存失效
            cacheClient.deleteByPrefix(CACHE_SPECIAL_LIST_HOSPITAL_KEY);
            return true;
        }
        return false;
    }

    /**
     * 删除从医院中移除专科
     *
     * @param hospitalId 医院编号
     * @param specialId  专科编号
     * @return 是否成功
     */
    @Override
    public boolean deleteSpecialRelation(Long hospitalId, Long specialId) {

        HospitalSpecialRelationExample example = new HospitalSpecialRelationExample();

        example.createCriteria()
                .andHospitalIdEqualTo(hospitalId)
                .andSpecialIdEqualTo(specialId);

        if (specialRelationMapper.deleteByExample(example) > 0) {
            cacheClient.deleteByPrefix(CACHE_SPECIAL_LIST_HOSPITAL_KEY);
            return true;
        }
        return false;
    }

    /**
     * 判断关系是否存在
     *
     * @param id 关系编号
     * @return 是否存在
     */
    @Override
    public boolean countSpecialRelation(Long id) {
        HospitalSpecialRelationExample example = new HospitalSpecialRelationExample();

        example.createCriteria()
                .andIdEqualTo(id);

        return specialRelationMapper.countByExample(example) > 0;
    }

    /**
     * 判断医院是否存在该专科
     *
     * @param param 医院专科关系参数
     * @return 是否存在
     */
    @Override
    public boolean countSpecialRelation(HospitalSpecialRelationParam param) {
        HospitalSpecialRelationExample example = new HospitalSpecialRelationExample();

        example.createCriteria()
                .andHospitalIdEqualTo(param.getHospitalId())
                .andSpecialIdEqualTo(param.getSpecialId());

        return specialRelationMapper.countByExample(example) > 0;
    }

}
