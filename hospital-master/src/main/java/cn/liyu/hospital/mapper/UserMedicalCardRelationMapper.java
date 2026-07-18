package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.UserMedicalCardRelation;
import cn.liyu.hospital.entity.UserMedicalCardRelationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMedicalCardRelationMapper {
    long countByExample(UserMedicalCardRelationExample example);

    int deleteByExample(UserMedicalCardRelationExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UserMedicalCardRelation row);

    int insertSelective(UserMedicalCardRelation row);

    List<UserMedicalCardRelation> selectByExample(UserMedicalCardRelationExample example);

    UserMedicalCardRelation selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") UserMedicalCardRelation row, @Param("example") UserMedicalCardRelationExample example);

    int updateByExample(@Param("row") UserMedicalCardRelation row, @Param("example") UserMedicalCardRelationExample example);

    int updateByPrimaryKeySelective(UserMedicalCardRelation row);

    int updateByPrimaryKey(UserMedicalCardRelation row);
}