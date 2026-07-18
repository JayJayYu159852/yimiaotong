package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.LogOperation;
import cn.liyu.hospital.entity.LogOperationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface LogOperationMapper {
    long countByExample(LogOperationExample example);

    int deleteByExample(LogOperationExample example);

    int deleteByPrimaryKey(Long id);

    int insert(LogOperation row);

    int insertSelective(LogOperation row);

    List<LogOperation> selectByExampleWithBLOBs(LogOperationExample example);

    List<LogOperation> selectByExample(LogOperationExample example);

    LogOperation selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") LogOperation row, @Param("example") LogOperationExample example);

    int updateByExampleWithBLOBs(@Param("row") LogOperation row, @Param("example") LogOperationExample example);

    int updateByExample(@Param("row") LogOperation row, @Param("example") LogOperationExample example);

    int updateByPrimaryKeySelective(LogOperation row);

    int updateByPrimaryKeyWithBLOBs(LogOperation row);

    int updateByPrimaryKey(LogOperation row);
}