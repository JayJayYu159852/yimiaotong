package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PowerAccountRoleRelation;
import cn.liyu.hospital.entity.PowerAccountRoleRelationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PowerAccountRoleRelationMapper {
    long countByExample(PowerAccountRoleRelationExample example);

    int deleteByExample(PowerAccountRoleRelationExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PowerAccountRoleRelation row);

    int insertSelective(PowerAccountRoleRelation row);

    List<PowerAccountRoleRelation> selectByExample(PowerAccountRoleRelationExample example);

    PowerAccountRoleRelation selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PowerAccountRoleRelation row, @Param("example") PowerAccountRoleRelationExample example);

    int updateByExample(@Param("row") PowerAccountRoleRelation row, @Param("example") PowerAccountRoleRelationExample example);

    int updateByPrimaryKeySelective(PowerAccountRoleRelation row);

    int updateByPrimaryKey(PowerAccountRoleRelation row);
}