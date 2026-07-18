package cn.liyu.hospital.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
/**
 * @author 医秒通
 */
@Schema(description = "出诊预约参数")
@Data
public class VisitAppointmentParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 出诊编号
     */
    /**
     *
     * @mbg.generated
     */
    @Schema(description = "出诊编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;
    /**
     * 就诊卡号
     */
    @Schema(description = "就诊卡号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long cardId;
    /**
     * 账号编号
     */
    @Schema(description = "账号编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long accountId;
    /**
     * 1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5： 11点~11点半，6： 11点半~12点，7：2点~2点半，8： 2点半~3点，9： 3点~3点半，10： 3点半~4点，11： 4点~4点半，12： 4点半~5点，13： 5点~5点半，14：5点半~6点
     */
    @Schema(description = "1： 8点半~9点，2： 9点~9点半，3： 9点半~10点，4： 10点~10点半，5： 11点~11点半，6： 11点半~12点，7：2点~2点半，8： 2点半~3点，9： 3点~3点半，10： 3点半~4点，11： 4点~4点半，12： 4点半~5点，13： 5点~5点半，14：5点半~6点", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer timePeriod;
}
