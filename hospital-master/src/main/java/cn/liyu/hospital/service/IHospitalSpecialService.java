package cn.liyu.hospital.service;

import cn.liyu.hospital.dto.param.HospitalSpecialParam;
import cn.liyu.hospital.entity.HospitalSpecial;

import java.util.List;
import java.util.Optional;

/**
 * @author 医秒通
 */

public interface IHospitalSpecialService {

    /**
     * 添加专科信息
     *
     * @param param 专科信息参数
     * @return 是否成功
     */
    boolean insert(HospitalSpecialParam param);

    /**
     * 更新专科信息
     *
     * @param id    专科编号
     * @param param 专科信息参数
     * @return 是否成功
     */
    boolean update(Long id, HospitalSpecialParam param);

    /**
     * 删除专科信息
     *
     * @param id 专科编号
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 获取专科信息
     *
     * @param id 专科编号
     * @return 专科信息
     */
    Optional<HospitalSpecial> getOptional(Long id);

    /**
     * 获取专科名称
     *
     * @param id 专科编号
     * @return 专科名称
     */
    String getName(Long id);

    /**
     * 判断专科信息是否存在
     *
     * @param id 专科信息
     * @return 是否存在
     */
    boolean count(Long id);

    /**
     * 判断专科信息是否存在
     *
     * @param name 专科名称
     * @return 是否存在
     */
    boolean count(String name);

    /**
     * 查找专科信息
     *
     * @param name     专科名称
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 专科列表
     */
    List<HospitalSpecial> list(String name, Integer pageNum, Integer pageSize);

    /**
     * 查找医院，所属专科信息
     *
     * @param hospitalId 医院编号
     * @param pageNum    第几页
     * @param pageSize   页大小
     * @return 专科列表
     */
    List<HospitalSpecial> list(Long hospitalId, Integer pageNum, Integer pageSize);
}
