package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.HospitalFollow;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 医院关注 Mapper
 *
 * @author 医秒通
 */
@Mapper
public interface HospitalFollowMapper {

    @Insert("INSERT INTO hospital_follow (user_id, follow_user_id, gmt_create) VALUES (#{userId}, #{hospitalId}, #{gmtCreate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HospitalFollow follow);

    @Delete("DELETE FROM hospital_follow WHERE user_id = #{userId} AND follow_user_id = #{hospitalId}")
    int deleteByUserIdAndHospitalId(@Param("userId") Long userId, @Param("hospitalId") Long hospitalId);

    @Select("SELECT COUNT(*) FROM hospital_follow WHERE user_id = #{userId} AND follow_user_id = #{hospitalId}")
    int countByUserIdAndHospitalId(@Param("userId") Long userId, @Param("hospitalId") Long hospitalId);

    @Select("SELECT follow_user_id FROM hospital_follow WHERE user_id = #{userId}")
    List<Long> selectHospitalIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM hospital_follow WHERE follow_user_id = #{hospitalId}")
    List<HospitalFollow> selectByHospitalId(@Param("hospitalId") Long hospitalId);
}
