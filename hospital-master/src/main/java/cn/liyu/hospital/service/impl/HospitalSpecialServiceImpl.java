package cn.liyu.hospital.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.liyu.hospital.component.CacheClient;
import cn.liyu.hospital.dto.param.HospitalSpecialParam;
import cn.liyu.hospital.entity.*;
import cn.liyu.hospital.mapper.HospitalSpecialMapper;
import cn.liyu.hospital.mapper.HospitalSpecialRelationMapper;
import cn.liyu.hospital.service.IHospitalSpecialService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 医秒通
 */
@Service
public class HospitalSpecialServiceImpl implements IHospitalSpecialService {

    @Resource
    private HospitalSpecialMapper specialMapper;

    @Resource
    private HospitalSpecialRelationMapper specialRelationMapper;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY = "cache:special:";

    /**
     * 专科列表缓存前缀（逻辑过期方案，写操作时按前缀删除）
     */
    private static final String CACHE_LIST_KEY = "cache:special:list:";

    /**
     * 列表逻辑过期时间（30分钟，专科数据几乎不变）
     */
    private static final long CACHE_LIST_TTL = 30L;

    private static final Random RANDOM = new Random();

    /**
     * 添加专科信息
     *
     * @param param 专科信息参数
     * @return 是否成功
     */
    @Override
    public boolean insert(HospitalSpecialParam param) {
        HospitalSpecial special = new HospitalSpecial();

        BeanUtils.copyProperties(param, special);

        special.setGmtCreate(new Date());
        special.setGmtModified(new Date());

        if (specialMapper.insertSelective(special) > 0) {
            // 新增专科 → 列表缓存全部失效
            cacheClient.deleteByPrefix(CACHE_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 更新专科信息
     *
     * @param id    专科编号
     * @param param 专科信息参数
     * @return 是否成功
     */
    @Override
    public boolean update(Long id, HospitalSpecialParam param) {
        HospitalSpecial special = new HospitalSpecial();

        BeanUtils.copyProperties(param, special);

        special.setId(id);
        special.setGmtModified(new Date());

        // 先更新数据库
        if (specialMapper.updateByPrimaryKeySelective(special) > 0) {
            // 再删除缓存（保证数据一致性）：详情缓存 + 列表缓存
            stringRedisTemplate.delete(CACHE_KEY + id);
            cacheClient.deleteByPrefix(CACHE_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 删除专科信息
     *
     * @param id 专科编号
     * @return 是否成功
     */
    @Override
    public boolean delete(Long id) {
        if (specialMapper.deleteByPrimaryKey(id) > 0) {
            stringRedisTemplate.delete(CACHE_KEY + id);
            cacheClient.deleteByPrefix(CACHE_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 获取专科名称
     *
     * @param id 专科编号
     * @return 专科名称
     */
    @Override
    public String getName(Long id) {

        Optional<HospitalSpecial> special = getOptional(id);

        if (special.isPresent()) {
            return special.get().getName();
        }

        return special.map(HospitalSpecial::getName).orElse("未知");
    }

    /**
     * 获取专科信息
     *
     * @param id 专科编号
     * @return 专科信息
     */
    @Override
    public Optional<HospitalSpecial> getOptional(Long id) {
        return Optional.ofNullable(cacheClient.queryWithMutex(
                CACHE_KEY, id, HospitalSpecial.class,
                specialMapper::selectByPrimaryKey, 30L + RANDOM.nextInt(10), TimeUnit.MINUTES));
    }

    /**
     * 判断专科信息是否存在
     *
     * @param id 专科信息
     * @return 是否存在
     */
    @Override
    public boolean count(Long id) {
        HospitalSpecialExample example = new HospitalSpecialExample();

        example.createCriteria()
                .andIdEqualTo(id);

        return specialMapper.countByExample(example) > 0;
    }

    /**
     * 判断专科信息是否存在
     *
     * @param name 专科名称
     * @return 是否存在
     */
    @Override
    public boolean count(String name) {
        HospitalSpecialExample example = new HospitalSpecialExample();

        example.createCriteria()
                .andNameEqualTo(name);

        return specialMapper.countByExample(example) > 0;
    }

    /**
     * 查找专科信息
     * <p>
     * 无搜索词的常规浏览走逻辑过期缓存；带名称搜索参数组合多、命中率低，直查数据库
     *
     * @param name     专科名称
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 专科列表
     */
    @Override
    public List<HospitalSpecial> list(String name, Integer pageNum, Integer pageSize) {

        if (StrUtil.isNotEmpty(name)) {
            return selectByName(name, pageNum, pageSize);
        }

        return cacheClient.queryPageWithLogicalExpire(
                CACHE_LIST_KEY + pageNum + ":" + pageSize, HospitalSpecial.class,
                () -> selectByName(null, pageNum, pageSize),
                CACHE_LIST_TTL, TimeUnit.MINUTES);
    }

    /**
     * 数据库查询：按名称模糊查找专科列表
     */
    private List<HospitalSpecial> selectByName(String name, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        HospitalSpecialExample example = new HospitalSpecialExample();

        if (StrUtil.isNotEmpty(name)) {
            example.createCriteria()
                    .andNameLike("%" + name + "%");
        }

        return specialMapper.selectByExample(example);
    }

    /**
     * 查找医院，所属专科信息
     * <p>
     * 医院-专科关系读多写少 → 逻辑过期缓存，关系变更时按前缀删除
     *
     * @param hospitalId 医院编号
     * @param pageNum    第几页
     * @param pageSize   页大小
     * @return 专科列表
     */
    @Override
    public List<HospitalSpecial> list(Long hospitalId, Integer pageNum, Integer pageSize) {

        return cacheClient.queryPageWithLogicalExpire(
                CACHE_LIST_KEY + "hospital:" + hospitalId + ":" + pageNum + ":" + pageSize,
                HospitalSpecial.class,
                () -> selectByHospital(hospitalId, pageNum, pageSize),
                CACHE_LIST_TTL, TimeUnit.MINUTES);
    }

    /**
     * 数据库查询：医院下的专科列表
     */
    private List<HospitalSpecial> selectByHospital(Long hospitalId, Integer pageNum, Integer pageSize) {

        HospitalSpecialRelationExample example = new HospitalSpecialRelationExample();

        example.createCriteria()
                .andHospitalIdEqualTo(hospitalId);

        List<Long> specialList = specialRelationMapper.selectByExample(example).stream()
                .map(HospitalSpecialRelation::getSpecialId)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(specialList)) {
            return null;
        }

        PageHelper.startPage(pageNum, pageSize);

        HospitalSpecialExample specialExample = new HospitalSpecialExample();

        specialExample.createCriteria()
                .andIdIn(specialList);

        return specialMapper.selectByExample(specialExample);
    }
}
