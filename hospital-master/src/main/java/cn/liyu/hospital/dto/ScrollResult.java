package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * Feed 流滚动分页结果（对标黑马点评 ScrollResult）
 *
 * @author 医秒通
 */
@Schema(description = "Feed流滚动分页结果")
@Data
public class ScrollResult {

    @Schema(description = "当前页数据")
    private List<?> list;

    @Schema(description = "本次查询的最小时间戳（下一页的 lastId）")
    private Long minTime;

    @Schema(description = "同 score 的偏移量")
    private Integer offset;
}
