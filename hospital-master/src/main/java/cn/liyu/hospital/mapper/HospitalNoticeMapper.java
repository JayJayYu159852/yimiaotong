package cn.liyu.hospital.mapper;

import cn.liyu.hospital.entity.HospitalNotice;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 医院健康资讯 Mapper
 *
 * @author 医秒通
 */
@Mapper
public interface HospitalNoticeMapper {

    @Insert("INSERT INTO hospital_notice (hospital_id, title, content, picture, gmt_create, gmt_modified) " +
            "VALUES (#{hospitalId}, #{title}, #{content}, #{picture}, #{gmtCreate}, #{gmtModified})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HospitalNotice notice);

    @Select("SELECT n.*, h.name as hospitalName FROM hospital_notice n LEFT JOIN hospital_info h ON n.hospital_id = h.id WHERE n.id = #{id}")
    HospitalNotice selectById(@Param("id") Long id);

    @Select("SELECT n.*, h.name as hospitalName FROM hospital_notice n LEFT JOIN hospital_info h ON n.hospital_id = h.id WHERE n.hospital_id = #{hospitalId} ORDER BY n.gmt_create DESC")
    List<HospitalNotice> selectByHospitalId(@Param("hospitalId") Long hospitalId);

    @Select("SELECT n.*, h.name as hospitalName FROM hospital_notice n LEFT JOIN hospital_info h ON n.hospital_id = h.id ORDER BY n.gmt_create DESC")
    List<HospitalNotice> selectAllOrderByTime();

    @Select("<script>SELECT n.*, h.name as hospitalName FROM hospital_notice n LEFT JOIN hospital_info h ON n.hospital_id = h.id WHERE n.hospital_id IN <foreach collection='hospitalIds' item='hid' open='(' separator=',' close=')'>#{hid}</foreach> ORDER BY n.gmt_create DESC</script>")
    List<HospitalNotice> selectByHospitalIds(@Param("hospitalIds") List<Long> hospitalIds);
}
