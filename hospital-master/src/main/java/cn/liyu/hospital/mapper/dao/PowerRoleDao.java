package cn.liyu.hospital.mapper.dao;

import cn.liyu.hospital.entity.PowerResource;

import java.util.List;

/**
 * @author 医秒通
 */

public interface PowerRoleDao {

    /**
     * 通过角色编号，获取资源列表
     *
     * @param roleId 角色编号
     * @return 资源列表
     */
    List<PowerResource> listResourceByRoleId(Long roleId);
}
