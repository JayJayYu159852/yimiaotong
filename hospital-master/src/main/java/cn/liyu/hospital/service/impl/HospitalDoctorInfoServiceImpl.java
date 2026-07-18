package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.component.CacheClient;
import cn.liyu.hospital.dto.HospitalDoctorDTO;
import cn.liyu.hospital.dto.param.HospitalDoctorParam;
import cn.liyu.hospital.entity.HospitalDoctor;
import cn.liyu.hospital.entity.HospitalDoctorExample;
import cn.liyu.hospital.mapper.HospitalDoctorMapper;
import cn.liyu.hospital.service.IHospitalDoctorService;
import cn.liyu.hospital.service.IHospitalSpecialService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

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
public class HospitalDoctorInfoServiceImpl implements IHospitalDoctorService {

    @Resource
    private HospitalDoctorMapper doctorInfoMapper;

    @Resource
    private IHospitalSpecialService specialService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_DOCTOR_KEY = "cache:doctor:";

    /**
     * 医生列表缓存前缀（互斥锁 + TTL 方案，写操作时按前缀删除）
     */
    private static final String CACHE_DOCTOR_LIST_KEY = "cache:doctor:list:";

    /**
     * 列表缓存时间（10分钟，医生信息偶有变动，TTL 兜底 + 写时删除双保险）
     */
    private static final long CACHE_LIST_TTL = 10L;

    private static final Random RANDOM = new Random();

    /**
     * 添加医生信息
     *
     * @param param 医生信息参数
     * @return 是否成功
     */
    @Override
    public boolean insert(HospitalDoctorParam param) {
        HospitalDoctor info = new HospitalDoctor();

        BeanUtils.copyProperties(param, info);

        info.setGmtCreate(new Date());
        info.setGmtModified(new Date());

        if (doctorInfoMapper.insertSelective(info) > 0) {
            // 新增医生 → 列表缓存全部失效
            cacheClient.deleteByPrefix(CACHE_DOCTOR_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 更新医生信息
     *
     * @param id    医生编号
     * @param param 医生信息参数
     * @return 是否成功
     */
    @Override
    public boolean update(Long id, HospitalDoctorParam param) {
        HospitalDoctor info = new HospitalDoctor();

        BeanUtils.copyProperties(param, info);

        info.setId(id);
        info.setGmtModified(new Date());

        // 先更新数据库
        if (doctorInfoMapper.updateByPrimaryKeySelective(info) > 0) {
            // 再删除缓存（保证数据一致性）：详情缓存 + 列表缓存
            stringRedisTemplate.delete(CACHE_DOCTOR_KEY + id);
            cacheClient.deleteByPrefix(CACHE_DOCTOR_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 是否存在医生信息
     *
     * @param id 医生编号
     * @return 是否存在
     */
    @Override
    public boolean count(Long id) {
        HospitalDoctorExample example = new HospitalDoctorExample();

        example.createCriteria()
                .andIdEqualTo(id);

        return doctorInfoMapper.countByExample(example) > 0;
    }

    /**
     * 获取转换后的对象信息
     *
     * @param id 医生编号
     * @return 转换后的对象
     */
    @Override
    public Optional<HospitalDoctorDTO> getConvert(Long id) {
        return Optional.ofNullable(convert(doctorInfoMapper.selectByPrimaryKey(id)));
    }

    /**
     * 获取医生名称
     *
     * @param id 医生编号
     * @return 医生名称，空则，返回未知
     */
    @Override
    public String getName(Long id) {
        return getOptional(id).map(HospitalDoctor::getName).orElse("未知");
    }

    /**
     * 获取医生信息
     *
     * @param id 医生编号
     * @return 医生编号
     */
    @Override
    public Optional<HospitalDoctor> getOptional(Long id) {
        return Optional.ofNullable(cacheClient.queryWithMutex(
                CACHE_DOCTOR_KEY, id, HospitalDoctor.class,
                doctorInfoMapper::selectByPrimaryKey, 30L + RANDOM.nextInt(10), TimeUnit.MINUTES));
    }

    /**
     * 删除医生信息
     *
     * @param id 医生编号
     * @return 是否成功
     */
    @Override
    public boolean delete(Long id) {
        if (doctorInfoMapper.deleteByPrimaryKey(id) > 0) {
            stringRedisTemplate.delete(CACHE_DOCTOR_KEY + id);
            cacheClient.deleteByPrefix(CACHE_DOCTOR_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 查找医生信息
     * <p>
     * 无搜索词的常规浏览走互斥锁缓存（10分钟 TTL + 写时删除）；带名称搜索直查数据库
     *
     * @param name     医生名称
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 医生信息列表
     */
    @Override
    public List<HospitalDoctorDTO> list(String name, Integer pageNum, Integer pageSize) {

        if (StrUtil.isNotEmpty(name)) {
            return selectByName(name, pageNum, pageSize);
        }

        return cacheClient.queryPageWithMutex(
                CACHE_DOCTOR_LIST_KEY + pageNum + ":" + pageSize, HospitalDoctorDTO.class,
                () -> selectByName(null, pageNum, pageSize),
                CACHE_LIST_TTL, TimeUnit.MINUTES);
    }

    /**
     * 数据库查询：按名称模糊查找医生列表
     */
    private List<HospitalDoctorDTO> selectByName(String name, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        HospitalDoctorExample example = new HospitalDoctorExample();

        if (StrUtil.isNotEmpty(name)) {
            example.createCriteria()
                    .andNameLike("%" + name + "%");
        }

        return doctorInfoMapper.selectByExample(example).stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    /**
     * 查找医生信息列表
     * <p>
     * 挂号主链路查询（选专科→看医生）→ 互斥锁缓存；带名称搜索直查数据库
     *
     * @param name      医生名称
     * @param specialId 专科编号
     * @param pageNum   第几页
     * @param pageSize  页大小
     * @return 医生信息列表
     */
    @Override
    public List<HospitalDoctorDTO> list(String name, Long specialId, Integer pageNum, Integer pageSize) {

        if (StrUtil.isNotEmpty(name)) {
            return selectByCondition(name, specialId, pageNum, pageSize);
        }

        String key = CACHE_DOCTOR_LIST_KEY + "special:" + (specialId == null ? 0 : specialId)
                + ":" + pageNum + ":" + pageSize;

        return cacheClient.queryPageWithMutex(key, HospitalDoctorDTO.class,
                () -> selectByCondition(null, specialId, pageNum, pageSize),
                CACHE_LIST_TTL, TimeUnit.MINUTES);
    }

    /**
     * 数据库查询：按名称/专科组合查找医生列表
     */
    private List<HospitalDoctorDTO> selectByCondition(String name, Long specialId,
                                                      Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        HospitalDoctorExample example = new HospitalDoctorExample();

        HospitalDoctorExample.Criteria criteria = example.createCriteria();

        if (StrUtil.isNotEmpty(name)) {
            criteria.andNameLike("%" + name + "%");
        }

        if (specialId != null) {
            criteria.andSpecialIdEqualTo(specialId);
        }

        return doctorInfoMapper.selectByExample(example).stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    /**
     * 转换医生信息
     * 增加专科名称
     *
     * @param doctor 医生信息
     * @return 医生信息封装对象
     */
    private HospitalDoctorDTO convert(HospitalDoctor doctor) {

        HospitalDoctorDTO dto = new HospitalDoctorDTO();

        BeanUtils.copyProperties(doctor, dto);

        // 设置专科名称
        dto.setSpecialName(specialService.getName(doctor.getSpecialId()));

        return dto;
    }
}
