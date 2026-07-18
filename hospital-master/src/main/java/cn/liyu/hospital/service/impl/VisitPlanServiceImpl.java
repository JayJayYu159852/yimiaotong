package cn.liyu.hospital.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.liyu.hospital.component.CacheClient;
import cn.liyu.hospital.dto.*;
import cn.liyu.hospital.dto.param.VisitPlanParam;
import cn.liyu.hospital.dto.param.VisitPlanUpdateParam;
import cn.liyu.hospital.entity.VisitPlan;
import cn.liyu.hospital.entity.VisitPlanExample;
import cn.liyu.hospital.mapper.VisitPlanMapper;
import cn.liyu.hospital.service.*;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 医秒通
 */

@Service
public class VisitPlanServiceImpl implements IVisitPlanService {

    /**
     * 每段时间内 最大就诊人数
     */
    private static final Integer MAX_OF_PATIENTS = 5;

    /**
     * 排班搜索列表缓存前缀（互斥锁 + 短 TTL，排班变更时按前缀删除）
     */
    private static final String CACHE_PLAN_LIST_KEY = "cache:plan:list:";

    /**
     * 排班列表缓存时间（2分钟短 TTL：排班白天读多写少，但需较快感知管理端调整）
     */
    private static final long CACHE_PLAN_LIST_TTL = 2L;

    @Resource
    private VisitPlanMapper planMapper;

    @Resource
    private IHospitalDoctorService hospitalDoctorService;

    @Resource
    private IHospitalInfoService hospitalInfoService;

    @Resource
    private IHospitalSpecialService hospitalSpecialService;

    @Resource
    private IVisitAppointmentService appointmentService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 创建出诊计划
     *
     * @param param 出诊计划参数
     * @return 是否成功
     */
    @Override
    public boolean insertAll(VisitPlanParam param) {

        VisitPlan plan = new VisitPlan();

        BeanUtils.copyProperties(param, plan);

        plan.setGmtCreate(new Date());
        plan.setGmtModified(new Date());

        if (planMapper.insertSelective(plan) > 0) {
            // 新增排班 → 排班搜索缓存全部失效
            cacheClient.deleteByPrefix(CACHE_PLAN_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 更新出诊计划
     *
     * @param id    记录编号
     * @param param 出诊计划参数
     * @return 是否成功
     */
    @Override
    public boolean update(Long id, VisitPlanUpdateParam param) {

        VisitPlan plan = new VisitPlan();

        BeanUtils.copyProperties(param, plan);

        plan.setId(id);
        plan.setGmtModified(new Date());

        // 先更新数据库，再删除缓存（保证数据一致性）
        if (planMapper.updateByPrimaryKeySelective(plan) > 0) {
            cacheClient.deleteByPrefix(CACHE_PLAN_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 获取出诊计划
     *
     * @param id 计划编号
     * @return 出诊计划
     */
    @Override
    public Optional<VisitPlanDTO> getOptional(Long id) {
        return Optional.ofNullable(covert(planMapper.selectByPrimaryKey(id)));
    }

    /**
     * 删除出诊计划
     *
     * @param idList 计划编号
     * @return 是否成功
     */
    @Override
    public boolean deleteAll(List<Long> idList) {
        VisitPlanExample example = new VisitPlanExample();

        example.createCriteria()
                .andIdIn(idList);

        if (planMapper.deleteByExample(example) > 0) {
            cacheClient.deleteByPrefix(CACHE_PLAN_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 删除出诊计划
     *
     * @param id 计划编号
     * @return 是否成功
     */
    @Override
    public boolean delete(Long id) {
        if (planMapper.deleteByPrimaryKey(id) > 0) {
            cacheClient.deleteByPrefix(CACHE_PLAN_LIST_KEY);
            return true;
        }
        return false;
    }

    /**
     * 判断是否，存在该计划
     *
     * @param id 计划编号
     * @return 是否存在
     */
    @Override
    public boolean count(Long id) {

        VisitPlanExample example = new VisitPlanExample();

        example.createCriteria()
                .andIdEqualTo(id);

        return planMapper.countByExample(example) > 0;
    }

    /**
     * 获取医生出诊信息
     *
     * @param doctorId 医生编号
     * @param start    开始日期
     * @param end      结束日期
     * @return 医生出诊信息
     */
    @Override
    public VisitDoctorPlanDTO getDoctorPlan(Long doctorId, Date start, Date end) {

        VisitDoctorPlanDTO dto = new VisitDoctorPlanDTO();

        // 设置医生信息
        if (hospitalDoctorService.getConvert(doctorId).isPresent()) {
            dto.setDoctorDTO(hospitalDoctorService.getConvert(doctorId).get());
        }

        // 设置医生出诊信息列表
        dto.setPlanListDTOList(getVisitPlanDTO(doctorId, start, end));

        return dto;
    }

    /**
     * 获取医生出诊信息
     *
     * @param hospitalId 医院编号
     * @param doctorId   医生编号
     * @param date       日期
     * @return 医生出诊信息
     */
    @Override
    public List<VisitPlanResiduesDTO> getDoctorPlanByDate(Long hospitalId, Long doctorId, Date date) {

        // 1. 优先按医院+医生精确查询当天出诊计划
        VisitPlanExample example = new VisitPlanExample();
        example.createCriteria()
                .andDoctorIdEqualTo(doctorId)
                .andHospitalIdEqualTo(hospitalId)
                .andDayBetween(DateUtil.beginOfDay(date), DateUtil.endOfDay(date));

        List<VisitPlan> list = planMapper.selectByExample(example);

        // 2. 精确匹配为空时，退化为仅按医生+日期查询（兼容医院 ID 不一致的场景，与秒杀页数据统一）
        if (CollUtil.isEmpty(list)) {
            VisitPlanExample fallbackExample = new VisitPlanExample();
            fallbackExample.createCriteria()
                    .andDoctorIdEqualTo(doctorId)
                    .andDayBetween(DateUtil.beginOfDay(date), DateUtil.endOfDay(date));
            list = planMapper.selectByExample(fallbackExample);
        }

        if (CollUtil.isEmpty(list)) {
            return null;
        }

        return list.stream()
                .map(this::covertToResidues)
                .collect(Collectors.toList());
    }

    /**
     * 查找出诊列表
     * <p>
     * 挂号主链路查询（按医院/专科/日期搜排班）→ 互斥锁缓存 + 2分钟短 TTL：
     * 排班本身不含号源余量（余量走 Redis 秒杀库存），可安全缓存；短 TTL 兜底 + 排班变更删缓存
     *
     * @param hospitalId 医院编号
     * @param specialId  专科编号
     * @param doctorId   医生编号
     * @param day        出诊日期
     * @param pageNum    第几页
     * @param pageSize   页大小
     * @return 出诊列表
     */
    @Override
    public List<VisitPlanDTO> list(Long hospitalId, Long specialId, Long doctorId, Date day,
                                   Integer pageNum, Integer pageSize) {

        String key = CACHE_PLAN_LIST_KEY
                + (hospitalId == null ? 0 : hospitalId)
                + ":" + (specialId == null ? 0 : specialId)
                + ":" + (doctorId == null ? 0 : doctorId)
                + ":" + DateUtil.format(day, "yyyyMMdd")
                + ":" + pageNum + ":" + pageSize;

        return cacheClient.queryPageWithMutex(key, VisitPlanDTO.class,
                () -> selectPlanList(hospitalId, specialId, doctorId, day, pageNum, pageSize),
                CACHE_PLAN_LIST_TTL, TimeUnit.MINUTES);
    }

    /**
     * 数据库查询：按条件搜索出诊列表
     */
    private List<VisitPlanDTO> selectPlanList(Long hospitalId, Long specialId, Long doctorId,
                                              Date day, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        VisitPlanExample example = new VisitPlanExample();

        VisitPlanExample.Criteria criteria = example.createCriteria();

        if (hospitalId != null) {
            criteria.andHospitalIdEqualTo(hospitalId);
        }

        if (specialId != null) {
            criteria.andSpecialIdEqualTo(specialId);
        }

        if (doctorId != null) {
            criteria.andDoctorIdEqualTo(doctorId);
        }

        criteria.andDayBetween(DateUtil.beginOfDay(day), DateUtil.endOfDay(day));

        return toDtoPage(planMapper.selectByExample(example));
    }

    @Override
    public List<VisitPlanDTO> listAll(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        VisitPlanExample example = new VisitPlanExample();
        // 只展示今天及之后的出诊计划，按日期升序 → 与用户端一致
        example.createCriteria()
                .andDayGreaterThanOrEqualTo(DateUtil.beginOfDay(new Date()));
        example.setOrderByClause("day asc, time asc");

        return toDtoPage(planMapper.selectByExample(example));
    }

    /**
     * 转换为 DTO 列表：若源列表为 PageHelper 分页对象，保留分页元数据
     * <p>
     * stream().collect 会把 Page 打回普通 ArrayList，导致 CommonPage.restPage
     * 取不到 total/pages（总数变成当页条数、前端无法翻页），故转换后还原 Page
     *
     * @param plans 数据库查询结果（可能为 Page）
     * @return DTO 列表（保留分页元数据）
     */
    private List<VisitPlanDTO> toDtoPage(List<VisitPlan> plans) {
        List<VisitPlanDTO> dtoList = plans.stream()
                .map(this::covert)
                .collect(Collectors.toList());

        if (plans instanceof Page) {
            Page<VisitPlan> source = (Page<VisitPlan>) plans;
            Page<VisitPlanDTO> dtoPage = new Page<>(source.getPageNum(), source.getPageSize());
            dtoPage.setTotal(source.getTotal());
            dtoPage.setPages(source.getPages());
            dtoPage.addAll(dtoList);
            return dtoPage;
        }
        return dtoList;
    }


    /**
     * 获取某段时间出诊计划
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 出诊计划编号
     */
    @Override
    public List<Long> list(Date start, Date end) {
        VisitPlanExample example = new VisitPlanExample();

        example.createCriteria()
                .andDayGreaterThanOrEqualTo(start)
                .andDayLessThanOrEqualTo(end);

        return planMapper.selectByExample(example).stream()
                .map(VisitPlan::getId).collect(Collectors.toList());
    }

    /**
     * 按医生/日期查询出诊计划编号列表（管理端预约列表筛选用）
     *
     * @param doctorId 医生编号（可空）
     * @param day      出诊日期（可空）
     * @return 出诊计划编号列表
     */
    @Override
    public List<Long> listPlanIds(Long doctorId, Date day) {
        VisitPlanExample example = new VisitPlanExample();

        VisitPlanExample.Criteria criteria = example.createCriteria();

        if (doctorId != null) {
            criteria.andDoctorIdEqualTo(doctorId);
        }

        if (day != null) {
            criteria.andDayBetween(DateUtil.beginOfDay(day), DateUtil.endOfDay(day));
        }

        return planMapper.selectByExample(example).stream()
                .map(VisitPlan::getId)
                .collect(Collectors.toList());
    }

    /**
     * 获取某天出诊计划信息
     *
     * @param doctorId 医生编号
     * @param time     时间段：1 上午，2 下午
     * @param day      某天
     * @return 出诊计划列表
     */
    @Override
    public List<VisitPlan> getByTimeAndDate(Long doctorId, Integer time, Date day) {
        VisitPlanExample example = new VisitPlanExample();

        example.createCriteria()
                .andDoctorIdEqualTo(doctorId)
                .andTimeEqualTo(time)
                .andDayGreaterThanOrEqualTo(DateUtil.beginOfDay(day))
                .andDayLessThanOrEqualTo(DateUtil.endOfDay(day));

        return planMapper.selectByExample(example);
    }

    /**
     * 转换为出诊计划封装类
     * 增加诊室地址、医生名称
     *
     * @param plan 出诊计划
     * @return 出诊计划封装类
     */
    private VisitPlanDTO covert(VisitPlan plan) {
        VisitPlanDTO dto = new VisitPlanDTO();

        BeanUtils.copyProperties(plan, dto);

        // 设置医生名称
        dto.setDoctorName(hospitalDoctorService.getName(plan.getDoctorId()));

        // 设置专科名称
        dto.setSpecialName(hospitalSpecialService.getName(plan.getSpecialId()));

        // 设置医院名称
        dto.setHospitalName(hospitalInfoService.getName(plan.getHospitalId()));

        return dto;
    }

    /**
     * 转换为出诊计划挂号封装类
     * 增加剩余挂号
     *
     * @param plan 出诊计划
     * @return 出诊计划封装类
     */
    private VisitPlanResiduesDTO covertToResidues(VisitPlan plan) {

        VisitPlanResiduesDTO dto = new VisitPlanResiduesDTO();

        BeanUtils.copyProperties(plan, dto);

        List<Integer> residueList = new ArrayList<>();

        // 优先从 Redis 秒杀库存读取剩余号数（与秒杀页数据统一）
        String stockKey = "seckill:stock:" + plan.getId();
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);

        if (stockStr != null) {
            residueList.add(Integer.parseInt(stockStr));
        } else {
            // Redis 无库存数据，回退到 DB 计算
            TimePeriodEnum timePeriod = (plan.getTime().equals(TimePeriodEnum.AM.getTime()))
                    ? TimePeriodEnum.AM : TimePeriodEnum.PM;
            for (int i = timePeriod.getStart(); i <= timePeriod.getEnd(); i++) {
                residueList.add(MAX_OF_PATIENTS - appointmentService.countByPlanId(plan.getId(), i));
            }
        }

        // 设置剩余号数
        dto.setResidues(residueList);

        return dto;
    }

    /**
     * 获取医生，某段时间，以后出诊信息列表
     *
     * @param doctorId 医生编号
     * @param start    获取该时间之后
     * @return 出诊列表
     */
    private List<VisitPlanListDTO> getVisitPlanDTO(Long doctorId, Date start, Date end) {

        // 查找医生出诊信息列表
        List<VisitPlanListDTO> planDTOList = new ArrayList<>();

        VisitPlanExample example = new VisitPlanExample();

        example.createCriteria()
                .andDoctorIdEqualTo(doctorId)
                .andDayBetween(DateUtil.beginOfDay(start), DateUtil.endOfDay(end));

        planMapper.selectByExample(example).stream()
                // 按照不同医院，进行分组
                .collect(Collectors.groupingBy(VisitPlan::getHospitalId, Collectors.toList()))

                // 按照不同医院，进行遍历
                .forEach((hospitalId, list) -> {

                    // 转换为医生出诊列表
                    VisitPlanListDTO dto = new VisitPlanListDTO();

                    // 设置医院信息
                    if (hospitalInfoService.getOptional(hospitalId).isPresent()) {
                        dto.setInfo(hospitalInfoService.getOptional(hospitalId).get());
                    }

                    // 设置医生出诊列表
                    if (CollUtil.isNotEmpty(list)) {
                        dto.setPlanResiduesDTOList(list.stream()
                                .map(this::covertToResidues)
                                .collect(Collectors.toList()));
                    }

                    // 设置对应医院中，医生出诊计划信息
                    planDTOList.add(dto);

                });

        return planDTOList;
    }

}
