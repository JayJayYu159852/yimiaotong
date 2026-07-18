package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.VisitBlacklist;
import cn.liyu.hospital.entity.VisitBlacklistExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface VisitBlacklistMapper {
    long countByExample(VisitBlacklistExample example);

    int deleteByExample(VisitBlacklistExample example);

    int deleteByPrimaryKey(Long id);

    int insert(VisitBlacklist row);

    int insertSelective(VisitBlacklist row);

    List<VisitBlacklist> selectByExample(VisitBlacklistExample example);

    VisitBlacklist selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("row") VisitBlacklist row, @Param("example") VisitBlacklistExample example);

    int updateByExample(@Param("row") VisitBlacklist row, @Param("example") VisitBlacklistExample example);

    int updateByPrimaryKeySelective(VisitBlacklist row);

    int updateByPrimaryKey(VisitBlacklist row);
}