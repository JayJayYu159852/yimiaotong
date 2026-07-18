package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.HospitalSpecial;
import cn.liyu.hospital.entity.HospitalSpecialExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HospitalSpecialMapper {
    long countByExample(HospitalSpecialExample example);

    int deleteByExample(HospitalSpecialExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HospitalSpecial row);

    int insertSelective(HospitalSpecial row);

    List<HospitalSpecial> selectByExample(HospitalSpecialExample example);

    HospitalSpecial selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") HospitalSpecial row, @Param("example") HospitalSpecialExample example);

    int updateByExample(@Param("row") HospitalSpecial row, @Param("example") HospitalSpecialExample example);

    int updateByPrimaryKeySelective(HospitalSpecial row);

    int updateByPrimaryKey(HospitalSpecial row);
}