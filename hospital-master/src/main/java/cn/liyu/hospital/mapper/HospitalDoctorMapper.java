package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.HospitalDoctor;
import cn.liyu.hospital.entity.HospitalDoctorExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HospitalDoctorMapper {
    long countByExample(HospitalDoctorExample example);

    int deleteByExample(HospitalDoctorExample example);

    int deleteByPrimaryKey(Long id);

    int insert(HospitalDoctor row);

    int insertSelective(HospitalDoctor row);

    List<HospitalDoctor> selectByExample(HospitalDoctorExample example);

    HospitalDoctor selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") HospitalDoctor row, @Param("example") HospitalDoctorExample example);

    int updateByExample(@Param("row") HospitalDoctor row, @Param("example") HospitalDoctorExample example);

    int updateByPrimaryKeySelective(HospitalDoctor row);

    int updateByPrimaryKey(HospitalDoctor row);
}