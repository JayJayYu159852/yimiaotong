package cn.liyu.hospital.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Feed 流单条资讯（对标黑马点评 Blog + User 聚合）
 *
 * @author 医秒通
 */
@Schema(description = "Feed流资讯条目")
@Data
@Builder
public class FeedItemDTO {

    @Schema(description = "资讯编号")
    private Long noticeId;

    @Schema(description = "医院编号")
    private Long hospitalId;

    @Schema(description = "医院名称")
    private String hospitalName;

    @Schema(description = "资讯标题")
    private String title;

    @Schema(description = "资讯内容摘要")
    private String content;

    @Schema(description = "发布时间戳")
    private Long timestamp;
}
