package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.HospitalSpecialRelation;
import cn.liyu.hospital.entity.HospitalSpecialRelationExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HospitalSpecialRelationMapper {
    long countByExample(HospitalSpecialRelationExample example);

    int deleteByExample(HospitalSpecialRelationExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HospitalSpecialRelation row);

    int insertSelective(HospitalSpecialRelation row);

    List<HospitalSpecialRelation> selectByExample(HospitalSpecialRelationExample example);

    HospitalSpecialRelation selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") HospitalSpecialRelation row, @Param("example") HospitalSpecialRelationExample example);

    int updateByExample(@Param("row") HospitalSpecialRelation row, @Param("example") HospitalSpecialRelationExample example);

    int updateByPrimaryKeySelective(HospitalSpecialRelation row);

    int updateByPrimaryKey(HospitalSpecialRelation row);
}