package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.UserMedicalCard;
import cn.liyu.hospital.entity.UserMedicalCardExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMedicalCardMapper {
    long countByExample(UserMedicalCardExample example);

    int deleteByExample(UserMedicalCardExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserMedicalCard row);

    int insertSelective(UserMedicalCard row);

    List<UserMedicalCard> selectByExample(UserMedicalCardExample example);

    UserMedicalCard selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserMedicalCard row, @Param("example") UserMedicalCardExample example);

    int updateByExample(@Param("row") UserMedicalCard row, @Param("example") UserMedicalCardExample example);

    int updateByPrimaryKeySelective(UserMedicalCard row);

    int updateByPrimaryKey(UserMedicalCard row);
}