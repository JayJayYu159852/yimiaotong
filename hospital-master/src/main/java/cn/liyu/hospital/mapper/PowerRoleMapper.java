package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PowerRole;
import cn.liyu.hospital.entity.PowerRoleExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PowerRoleMapper {
    long countByExample(PowerRoleExample example);

    int deleteByExample(PowerRoleExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PowerRole row);

    int insertSelective(PowerRole row);

    List<PowerRole> selectByExample(PowerRoleExample example);

    PowerRole selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PowerRole row, @Param("example") PowerRoleExample example);

    int updateByExample(@Param("row") PowerRole row, @Param("example") PowerRoleExample example);

    int updateByPrimaryKeySelective(PowerRole row);

    int updateByPrimaryKey(PowerRole row);
}