package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.UserBasicInfo;
import cn.liyu.hospital.entity.UserBasicInfoExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserBasicInfoMapper {
    long countByExample(UserBasicInfoExample example);

    int deleteByExample(UserBasicInfoExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserBasicInfo row);

    int insertSelective(UserBasicInfo row);

    List<UserBasicInfo> selectByExample(UserBasicInfoExample example);

    UserBasicInfo selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserBasicInfo row, @Param("example") UserBasicInfoExample example);

    int updateByExample(@Param("row") UserBasicInfo row, @Param("example") UserBasicInfoExample example);

    int updateByPrimaryKeySelective(UserBasicInfo row);

    int updateByPrimaryKey(UserBasicInfo row);
}