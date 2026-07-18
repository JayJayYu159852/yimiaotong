package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
/**
 * @author 医秒通
 */
@Schema(description = "就诊用户信息")
@Data
public class VisitUserInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "预约编号")
    private Long appointmentId;
    /**
     * 就诊卡号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "就诊卡号")
    private Long cardId;
    @Schema(description = "就诊号")
    private Integer queueNum;
    /**
     * 姓名
     */
    @Schema(description = "姓名")
    private String name;
    /**
     * 性别 男：1，女：2
     */
    @Schema(description = "性别 男：1，女：2")
    private Integer gender;
    /**
     * 出生日期
     */
    @Schema(description = "出生日期")
    private Date birthDate;
    /**
     * 1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5: 10点半~11点，6： 11点~11点半，7： 11点半~12点，
     */
    /**
     * 8：2点~2点半，9： 2点半~3点，10： 3点~3点半，11： 3点半~4点，12： 4点~4点半，13： 4点半~5点，14： 5点~5点半，15：5点半~6点
     */
    @Schema(description = "1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5: 10点半~11点，6： 11点~11点半，7： 11点半~12点，8：2点~2点半，9： 2点半~3点，10： 3点~3点半，11： 3点半~4点，12： 4点~4点半，13： 4点半~5点，14： 5点~5点半，15：5点半~6点")
    private Integer timePeriod;
    /**
     * 预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成
     */
    @Schema(description = "预约状态 0：未开始，1：未按时就诊，2：取消预约挂号，3：已完成")
    private Integer status;
}
