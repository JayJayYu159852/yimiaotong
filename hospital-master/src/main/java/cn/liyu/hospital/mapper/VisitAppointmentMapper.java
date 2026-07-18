package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.VisitAppointment;
import cn.liyu.hospital.entity.VisitAppointmentExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface VisitAppointmentMapper {
    long countByExample(VisitAppointmentExample example);

    int deleteByExample(VisitAppointmentExample example);

    int deleteByPrimaryKey(Long id);

    int insert(VisitAppointment row);

    int insertSelective(VisitAppointment row);

    List<VisitAppointment> selectByExample(VisitAppointmentExample example);

    VisitAppointment selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") VisitAppointment row, @Param("example") VisitAppointmentExample example);

    int updateByExample(@Param("row") VisitAppointment row, @Param("example") VisitAppointmentExample example);

    int updateByPrimaryKeySelective(VisitAppointment row);

    int updateByPrimaryKey(VisitAppointment row);
}