package cn.liyu.hospital.service;

import cn.liyu.hospital.dto.param.HospitalInfoParam;
import cn.liyu.hospital.dto.param.HospitalSpecialRelationParam;
import cn.liyu.hospital.entity.HospitalInfo;

import java.util.List;
import java.util.Optional;

/**
 * @author 医秒通
 */

public interface IHospitalInfoService {

    /**
     * 添加医院信息
     *
     * @param param 医院信息参数
     * @return 新生成的医院编号（失败返回 null）
     */
    Long insert(HospitalInfoParam param);

    /**
     * 更新医院信息
     *
     * @param id    医院编号
     * @param param 医院信息参数
     * @return 是否成功
     */
    boolean update(Long id, HospitalInfoParam param);

    /**
     * 获取医院信息
     *
     * @param id 医院编号
     * @return 医院信息
     */
    Optional<HospitalInfo> getOptional(Long id);

    /**
     * 获取医院名称
     *
     * @param id 医院编号
     * @return 医院名称，否则返回未知
     */
    String getName(Long id);

    /**
     * 删除医院信息
     *
     * @param id 医院编号
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 判断医院信息是否存在
     *
     * @param id 医院编号
     * @return 是否存在
     */
    boolean count(Long id);

    /**
     * 判断电话是否存在
     *
     * @param phone 电话
     * @return 是否存在
     */
    boolean count(String phone);

    /**
     * 查找医院列表
     *
     * @param name     医院名称
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 医院列表
     */
    List<HospitalInfo> list(String name, Integer pageNum, Integer pageSize);

    /**
     * 插入专科到医院中去
     *
     * @param param 医院专科关系参数
     * @return 是否成功
     */
    boolean insertSpecialRelation(HospitalSpecialRelationParam param);

    /**
     * 删除从医院中移除专科
     *
     * @param hospitalId 医院编号
     * @param specialId  专科编号
     * @return 是否成功
     */
    boolean deleteSpecialRelation(Long hospitalId, Long specialId);

    /**
     * 判断关系是否存在
     *
     * @param id 关系编号
     * @return 是否存在
     */
    boolean countSpecialRelation(Long id);

    /**
     * 判断医院是否存在该专科
     *
     * @param param 医院专科关系参数
     * @return 是否存在
     */
    boolean countSpecialRelation(HospitalSpecialRelationParam param);
}
