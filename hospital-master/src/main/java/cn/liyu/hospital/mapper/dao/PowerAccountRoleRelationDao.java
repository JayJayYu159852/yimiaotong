package cn.liyu.hospital.mapper.dao;

import cn.liyu.hospital.entity.PowerAccountRoleRelation;
import cn.liyu.hospital.entity.PowerResource;
import cn.liyu.hospital.entity.PowerRole;

import java.util.List;

/**
 * @author 医秒通
 */

public interface PowerAccountRoleRelationDao {
    /**
     * 批量插入用户角色关系
     *
     * @param accountRoleRelationList 账号角色关系列表
     * @return 成功记录数
     */
    int insertList(List<PowerAccountRoleRelation> accountRoleRelationList);

    /**
     * 获取用于所有角色
     *
     * @param accountId 账号编号
     * @return 账号拥有角色列表
     */
    List<PowerRole> getRoleList(Long accountId);

    /**
     * 获取用户所有可访问资源
     *
     * @param accountId 账号编号
     * @return 资源列表
     */
    List<PowerResource> getResourceList(Long accountId);
}
