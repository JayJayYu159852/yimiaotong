package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PowerRoleResourceRelation;
import cn.liyu.hospital.entity.PowerRoleResourceRelationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PowerRoleResourceRelationMapper {
    long countByExample(PowerRoleResourceRelationExample example);

    int deleteByExample(PowerRoleResourceRelationExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PowerRoleResourceRelation row);

    int insertSelective(PowerRoleResourceRelation row);

    List<PowerRoleResourceRelation> selectByExample(PowerRoleResourceRelationExample example);

    PowerRoleResourceRelation selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PowerRoleResourceRelation row, @Param("example") PowerRoleResourceRelationExample example);

    int updateByExample(@Param("row") PowerRoleResourceRelation row, @Param("example") PowerRoleResourceRelationExample example);

    int updateByPrimaryKeySelective(PowerRoleResourceRelation row);

    int updateByPrimaryKey(PowerRoleResourceRelation row);
}