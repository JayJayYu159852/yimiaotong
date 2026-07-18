package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.LogAccountLogin;
import cn.liyu.hospital.entity.LogAccountLoginExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LogAccountLoginMapper {
    long countByExample(LogAccountLoginExample example);

    int deleteByExample(LogAccountLoginExample example);

    int deleteByPrimaryKey(Long id);

    int insert(LogAccountLogin row);

    int insertSelective(LogAccountLogin row);

    List<LogAccountLogin> selectByExample(LogAccountLoginExample example);

    LogAccountLogin selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") LogAccountLogin row, @Param("example") LogAccountLoginExample example);

    int updateByExample(@Param("row") LogAccountLogin row, @Param("example") LogAccountLoginExample example);

    int updateByPrimaryKeySelective(LogAccountLogin row);

    int updateByPrimaryKey(LogAccountLogin row);
}