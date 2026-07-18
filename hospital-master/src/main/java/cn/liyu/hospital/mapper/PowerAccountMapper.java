package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.PowerAccount;
import cn.liyu.hospital.entity.PowerAccountExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PowerAccountMapper {
    long countByExample(PowerAccountExample example);

    int deleteByExample(PowerAccountExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PowerAccount row);

    int insertSelective(PowerAccount row);

    List<PowerAccount> selectByExample(PowerAccountExample example);

    PowerAccount selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") PowerAccount row, @Param("example") PowerAccountExample example);

    int updateByExample(@Param("row") PowerAccount row, @Param("example") PowerAccountExample example);

    int updateByPrimaryKeySelective(PowerAccount row);

    int updateByPrimaryKey(PowerAccount row);
}