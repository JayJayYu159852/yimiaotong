package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.HospitalInfo;
import cn.liyu.hospital.entity.HospitalInfoExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HospitalInfoMapper {
    long countByExample(HospitalInfoExample example);

    int deleteByExample(HospitalInfoExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HospitalInfo row);

    int insertSelective(HospitalInfo row);

    List<HospitalInfo> selectByExample(HospitalInfoExample example);

    HospitalInfo selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") HospitalInfo row, @Param("example") HospitalInfoExample example);

    int updateByExample(@Param("row") HospitalInfo row, @Param("example") HospitalInfoExample example);

    int updateByPrimaryKeySelective(HospitalInfo row);

    int updateByPrimaryKey(HospitalInfo row);
}