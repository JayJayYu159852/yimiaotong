package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.VisitPlan;
import cn.liyu.hospital.entity.VisitPlanExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface VisitPlanMapper {
    long countByExample(VisitPlanExample example);

    int deleteByExample(VisitPlanExample example);

    int deleteByPrimaryKey(Long id);

    int insert(VisitPlan row);

    int insertSelective(VisitPlan row);

    List<VisitPlan> selectByExample(VisitPlanExample example);

    VisitPlan selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") VisitPlan row, @Param("example") VisitPlanExample example);

    int updateByExample(@Param("row") VisitPlan row, @Param("example") VisitPlanExample example);

    int updateByPrimaryKeySelective(VisitPlan row);

    int updateByPrimaryKey(VisitPlan row);
}