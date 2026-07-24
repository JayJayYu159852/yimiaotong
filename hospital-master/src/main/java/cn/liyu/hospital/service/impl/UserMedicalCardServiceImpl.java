package cn.liyu.hospital.service.impl;

import cn.liyu.hospital.dto.UserMedicalCardDTO;
import cn.liyu.hospital.dto.param.UserMedicalCardParam;
import cn.liyu.hospital.dto.param.UserMedicalCardUpdateParam;
import cn.liyu.hospital.entity.UserMedicalCard;
import cn.liyu.hospital.entity.UserMedicalCardExample;
import cn.liyu.hospital.entity.UserMedicalCardRelation;
import cn.liyu.hospital.entity.UserMedicalCardRelationExample;
import cn.liyu.hospital.mapper.UserMedicalCardMapper;
import cn.liyu.hospital.mapper.UserMedicalCardRelationMapper;
import cn.liyu.hospital.service.IUserMedicalCardService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 医秒通
 */

@Service
public class UserMedicalCardServiceImpl implements IUserMedicalCardService {

    private static final int BOY = 1;

    private static final int GIRL = 2;

    @Resource
    private UserMedicalCardMapper medicalCardMapper;

    @Resource
    private UserMedicalCardRelationMapper relationMapper;

    /**
     * 添加医疗卡
     *
     * @param accountId 账号编号
     * @param param     就诊卡参数
     * @return 是否成功
     */
    @Override
    public boolean insert(Long accountId, UserMedicalCardParam param) {

        // 就诊卡，存在
        if (countIdentificationNumber(param.getIdentificationNumber())) {

            Optional<UserMedicalCard> cardOptional = getOptional(param.getIdentificationNumber());

            if (cardOptional.isPresent()) {
                return insertRelation(accountId, cardOptional.get().getId());
            }
        }

        // 就诊卡，不存在
        if (insertCard(param)) {
            // 获取就诊卡信息
            Optional<UserMedicalCard> cardOptional2 = getOptional(param.getIdentificationNumber());

            // 插入关系
            return cardOptional2.filter(userMedicalCard -> insertRelation(accountId, userMedicalCard.getId()))
                    .isPresent();
        }

        return false;
    }

    /**
     * 更新医疗卡（关系类型、姓名、电话）
     *
     * @param relationId 关系编号
     * @param param      就诊卡更新参数
     * @return 是否成功
     */
    @Override
    public boolean update(Long relationId, UserMedicalCardUpdateParam param) {

        UserMedicalCardRelation relation = relationMapper.selectByPrimaryKey(relationId);
        Long cardId = relation.getCardId();

        // 若身份证号有变更，检查是否已被其他卡占用
        if (StrUtil.isNotBlank(param.getIdentificationNumber())) {
            UserMedicalCard current = medicalCardMapper.selectByPrimaryKey(cardId);
            if (current != null && !param.getIdentificationNumber().equals(current.getIdentificationNumber())) {
                if (countIdentificationNumberExclude(param.getIdentificationNumber(), cardId)) {
                    return false;
                }
            }
        }

        // 更新信息参数
        UserMedicalCard card = new UserMedicalCard();

        BeanUtils.copyProperties(param, card);

        card.setId(cardId);
        card.setGmtModified(new Date());

        int count = medicalCardMapper.updateByPrimaryKeySelective(card);

        return count > 0;
    }

    /**
     * 获取患者名称
     *
     * @param id 就诊卡编号
     * @return 患者名称，或未知
     */
    @Override
    public String getName(Long id) {
        return getOptional(id).map(UserMedicalCard::getName).orElse("未知");
    }

    /**
     * 获取就诊卡信息
     *
     * @param id 就诊卡编号
     * @return 就诊卡信息
     */
    @Override
    public Optional<UserMedicalCard> getOptional(Long id) {

        return Optional.ofNullable(medicalCardMapper.selectByPrimaryKey(id));
    }

    /**
     * 通过身份证号码，获取就诊卡信息
     *
     * @param identificationNumber 身份证号码
     * @return 就诊卡信息
     */
    private Optional<UserMedicalCard> getOptional(String identificationNumber) {

        UserMedicalCardExample example = new UserMedicalCardExample();

        example.createCriteria()
                .andIdentificationNumberEqualTo(identificationNumber);

        return Optional.ofNullable(medicalCardMapper.selectByExample(example).get(0));
    }

    /**
     * 删除医疗卡
     *
     * @param relationId 关系编号
     * @return 是否成功
     */
    @Override
    public boolean delete(Long relationId) {
        return relationMapper.deleteByPrimaryKey(relationId) > 0;
    }

    /**
     * 查找就诊卡信息
     *
     * @param name     姓名
     * @param phone    手机号
     * @param gender   性别（ 0 所有，1 男，2 女）
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 就诊卡列表
     */
    @Override
    public List<UserMedicalCard> list(String name, String phone, Integer gender, Integer pageNum, Integer pageSize) {

        PageHelper.startPage(pageNum, pageSize);

        UserMedicalCardExample example = new UserMedicalCardExample();

        UserMedicalCardExample.Criteria criteria = example.createCriteria();

        if (StrUtil.isNotEmpty(name)) {
            criteria.andNameLike("%" + name + "%");
        }

        if (StrUtil.isNotEmpty(phone)) {
            criteria.andPhoneLike("%" + phone + "%");
        }

        if (gender == BOY || gender == GIRL) {
            criteria.andGenderEqualTo(gender);
        }

        return medicalCardMapper.selectByExample(example);
    }

    /**
     * 根据就诊卡编号
     *
     * @param idList 就诊卡编号
     * @return 用户就诊信息
     */
    @Override
    public List<UserMedicalCard> list(List<Long> idList) {

        UserMedicalCardExample example = new UserMedicalCardExample();

        example.createCriteria()
                .andIdIn(idList);

        return medicalCardMapper.selectByExample(example);
    }

    /**
     * 通过账号编号，获取就诊卡信息
     *
     * @param accountId 账号编号
     * @return 就诊卡列表
     */
    @Override
    public List<UserMedicalCardDTO> list(Long accountId) {

        UserMedicalCardRelationExample example = new UserMedicalCardRelationExample();

        example.createCriteria()
                .andAccountIdEqualTo(accountId);

        return relationMapper.selectByExample(example).stream()
                .map(this::covert)
                .collect(Collectors.toList());

    }

    /**
     * 通过账号编号获取唯一就诊卡编号（一人一卡）
     *
     * @param accountId 账号编号
     * @return 就诊卡编号
     */
    @Override
    public Optional<Long> getCardIdByAccountId(Long accountId) {

        UserMedicalCardRelationExample example = new UserMedicalCardRelationExample();

        example.createCriteria()
                .andAccountIdEqualTo(accountId);

        return relationMapper.selectByExample(example).stream()
                .findFirst()
                .map(UserMedicalCardRelation::getCardId);
    }

    private UserMedicalCardDTO covert(UserMedicalCardRelation relation) {
        UserMedicalCardDTO dto = new UserMedicalCardDTO();

        // 获取就诊卡信息
        Optional<UserMedicalCard> cardOptional = getOptional(relation.getCardId());

        // 复制就诊卡信息
        cardOptional.ifPresent(card -> BeanUtils.copyProperties(card, dto));

        // 复制关系相关字段
        BeanUtils.copyProperties(relation, dto);
        // 补充卡号
        dto.setId(relation.getCardId());
        dto.setRelationId(relation.getId());

        return dto;
    }

    /**
     * 判断关系编号是否存在
     *
     * @param relationId 关系编号
     * @return 是否存在
     */
    @Override
    public boolean countRelation(Long relationId) {

        UserMedicalCardRelationExample example = new UserMedicalCardRelationExample();

        example.createCriteria()
                .andIdEqualTo(relationId);


        return relationMapper.countByExample(example) > 0;
    }

    /**
     * 通过关系编号获取就诊卡编号
     *
     * @param relationId 关系编号
     * @return 就诊卡编号
     */
    @Override
    public Long getCardIdByRelationId(Long relationId) {
        return relationMapper.selectByPrimaryKey(relationId).getCardId();
    }

    /**
     * 统计用户绑定的就诊卡数量
     *
     * @param accountId 账号编号
     * @return 就诊卡数量
     */
    @Override
    public long count(Long accountId) {
        UserMedicalCardRelationExample example = new UserMedicalCardRelationExample();

        example.createCriteria()
                .andAccountIdEqualTo(accountId);


        return relationMapper.countByExample(example);
    }

    /**
     * 判断就诊卡信息是否存在
     *
     * @param identificationNumber 身份证编号
     * @return 是否存在
     */
    @Override
    public boolean countIdentificationNumber(String identificationNumber) {
        UserMedicalCardExample example = new UserMedicalCardExample();

        example.createCriteria()
                .andIdentificationNumberEqualTo(identificationNumber);

        return medicalCardMapper.countByExample(example) > 0;
    }

    /**
     * 判断身份证号是否已被其他就诊卡使用（排除当前卡）
     *
     * @param identificationNumber 身份证编号
     * @param excludeCardId        排除的当前卡编号
     * @return 是否已被其他卡使用
     */
    @Override
    public boolean countIdentificationNumberExclude(String identificationNumber, Long excludeCardId) {
        UserMedicalCardExample example = new UserMedicalCardExample();

        example.createCriteria()
                .andIdentificationNumberEqualTo(identificationNumber)
                .andIdNotEqualTo(excludeCardId);

        return medicalCardMapper.countByExample(example) > 0;
    }

    /**
     * 判断就诊卡号是否存在
     *
     * @param cardId 就诊卡号
     * @return 是否存在
     */
    @Override
    public boolean countCardId(Long cardId) {
        UserMedicalCardExample example = new UserMedicalCardExample();

        example.createCriteria()
                .andIdEqualTo(cardId);

        return medicalCardMapper.countByExample(example) > 0;
    }

    /**
     * 插入就诊卡关系（一人一卡，无需关系类型）
     *
     * @param accountId 用户账号编号
     * @param cardId    就诊卡
     * @return 是否成功
     */
    private boolean insertRelation(Long accountId, Long cardId) {
        UserMedicalCardRelation relation = new UserMedicalCardRelation();

        relation.setCardId(cardId);
        relation.setAccountId(accountId);

        relation.setGmtCreate(new Date());
        relation.setGmtModified(new Date());

        return relationMapper.insertSelective(relation) > 0;
    }

    /**
     * 注册时创建默认就诊卡（本人，姓名+手机号，其余信息待完善）
     */
    @Override
    public boolean initDefaultCard(Long accountId, String name, String phone) {
        UserMedicalCard card = new UserMedicalCard();
        card.setName(name);
        card.setPhone(phone);
        card.setGender(1); // 默认男，用户后续可修改
        card.setGmtCreate(new Date());
        card.setGmtModified(new Date());

        if (medicalCardMapper.insertSelective(card) > 0) {
            return insertRelation(accountId, card.getId());
        }
        return false;
    }

    /**
     * 添加医疗卡信息
     *
     * @param param 就诊卡参数
     * @return 是否成功
     */
    private boolean insertCard(UserMedicalCardParam param) {
        UserMedicalCard card = new UserMedicalCard();

        BeanUtils.copyProperties(param, card);

        card.setGmtCreate(new Date());
        card.setGmtModified(new Date());

        return medicalCardMapper.insertSelective(card) > 0;
    }
}
