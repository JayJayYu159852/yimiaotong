package cn.liyu.hospital.service;

import cn.liyu.hospital.dto.HospitalDoctorDTO;
import cn.liyu.hospital.dto.param.HospitalDoctorParam;
import cn.liyu.hospital.entity.HospitalDoctor;

import java.util.List;
import java.util.Optional;

/**
 * @author 医秒通
 */

public interface IHospitalDoctorService {

    /**
     * 添加医生信息
     *
     * @param param 医生信息参数
     * @return 是否成功
     */
    boolean insert(HospitalDoctorParam param);

    /**
     * 更新医生信息
     *
     * @param id    医生编号
     * @param param 医生信息参数
     * @return 是否成功
     */
    boolean update(Long id, HospitalDoctorParam param);

    /**
     * 是否存在医生信息
     *
     * @param id 医生编号
     * @return 是否存在
     */
    boolean count(Long id);

    /**
     * 获取医生信息
     *
     * @param id 医生编号
     * @return 医生编号
     */
    Optional<HospitalDoctor> getOptional(Long id);

    /**
     * 获取医生名称
     * @param id 医生编号
     * @return 医生名称，空则，返回未知
     */
    String getName(Long id);

    /**
     * 获取转换后的对象信息
     *
     * @param id 医生编号
     * @return 转换后的对象
     */
    Optional<HospitalDoctorDTO> getConvert(Long id);

    /**
     * 删除医生信息
     *
     * @param id 医生编号
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 查找医生信息
     *
     * @param name     医生名称
     * @param pageNum  第几页
     * @param pageSize 页大小
     * @return 医生信息列表
     */
    List<HospitalDoctorDTO> list(String name, Integer pageNum, Integer pageSize);

    /**
     * 查找医生信息列表
     *
     * @param name      医生名称
     * @param specialId 专科编号
     * @param pageNum   第几页
     * @param pageSize  页大小
     * @return 医生信息列表
     */
    List<HospitalDoctorDTO> list(String name, Long specialId, Integer pageNum, Integer pageSize);


}
