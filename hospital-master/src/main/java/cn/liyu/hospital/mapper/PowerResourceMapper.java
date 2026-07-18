package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PowerResource;
import cn.liyu.hospital.entity.PowerResourceExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PowerResourceMapper {
    long countByExample(PowerResourceExample example);

    int deleteByExample(PowerResourceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PowerResource row);

    int insertSelective(PowerResource row);

    List<PowerResource> selectByExample(PowerResourceExample example);

    PowerResource selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PowerResource row, @Param("example") PowerResourceExample example);

    int updateByExample(@Param("row") PowerResource row, @Param("example") PowerResourceExample example);

    int updateByPrimaryKeySelective(PowerResource row);

    int updateByPrimaryKey(PowerResource row);
}