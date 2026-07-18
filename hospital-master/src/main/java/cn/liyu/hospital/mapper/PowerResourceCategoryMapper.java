package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PowerResourceCategory;
import cn.liyu.hospital.entity.PowerResourceCategoryExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PowerResourceCategoryMapper {
    long countByExample(PowerResourceCategoryExample example);

    int deleteByExample(PowerResourceCategoryExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PowerResourceCategory row);

    int insertSelective(PowerResourceCategory row);

    List<PowerResourceCategory> selectByExample(PowerResourceCategoryExample example);

    PowerResourceCategory selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PowerResourceCategory row, @Param("example") PowerResourceCategoryExample example);

    int updateByExample(@Param("row") PowerResourceCategory row, @Param("example") PowerResourceCategoryExample example);

    int updateByPrimaryKeySelective(PowerResourceCategory row);

    int updateByPrimaryKey(PowerResourceCategory row);
}