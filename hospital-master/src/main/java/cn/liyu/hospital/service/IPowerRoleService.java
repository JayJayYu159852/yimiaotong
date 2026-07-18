package cn.liyu.hospital.service;

import cn.liyu.hospital.dto.param.PowerRoleParam;
import cn.liyu.hospital.dto.param.StatusParam;
import cn.liyu.hospital.entity.PowerResource;
import cn.liyu.hospital.entity.PowerRole;

import java.util.List;

/**
 * @author 医秒通
 */

public interface IPowerRoleService {

    /**
     * 添加角色
     *
     * @param param 角色信息参数
     * @return 成功记录数
     */
    boolean insert(PowerRoleParam param);

    /**
     * 修改角色信息
     *
     * @param roleId 角色编号
     * @param param  角色信息参数
     * @return 成功记录数
     */
    boolean update(Long roleId, PowerRoleParam param);


    /**
     * 更新角色状态
     *
     * @param roleId 角色编号
     * @param param  状态参数： 0 关闭，1 开启
     * @return 是否成功
     */
    boolean updateStatus(Long roleId, StatusParam param);

    /**
     * 删除角色信息
     *
     * @param roleId 角色编号
     * @return 是否成功
     */
    boolean delete(Long roleId);

    /**
     * 批量删除角色
     *
     * @param idList 角色编号
     * @return 成功记录数
     */
    int delete(List<Long> idList);

    /**
     * 获取角色列表
     *
     * @param chineseName 中文名
     * @param pageNum     第几页
     * @param pageSize    页大小
     * @return 角色列表
     */
    List<PowerRole> list(String chineseName, Integer pageNum, Integer pageSize);

    /**
     * 判断角色是否存在
     *
     * @param id 角色编号
     * @return 是否存在
     */
    boolean count(Long id);

    /**
     * 通过角色编号，获取资源列表
     *
     * @param roleId 角色编号
     * @return 资源列表
     */
    List<PowerResource> listMenuResource(Long roleId);

    /**
     * 更新角色资源列表
     *
     * @param roleId         角色编号
     * @param resourceIdList 资源列表
     * @return 是否成功
     */
    boolean allocResource(Long roleId, List<Long> resourceIdList);
}
