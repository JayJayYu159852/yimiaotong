package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.UserCase;
import cn.liyu.hospital.entity.UserCaseExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserCaseMapper {
    long countByExample(UserCaseExample example);

    int deleteByExample(UserCaseExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserCase row);

    int insertSelective(UserCase row);

    List<UserCase> selectByExample(UserCaseExample example);

    UserCase selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserCase row, @Param("example") UserCaseExample example);

    int updateByExample(@Param("row") UserCase row, @Param("example") UserCaseExample example);

    int updateByPrimaryKeySelective(UserCase row);

    int updateByPrimaryKey(UserCase row);
}