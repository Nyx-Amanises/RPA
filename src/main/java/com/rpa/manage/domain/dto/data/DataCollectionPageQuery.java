package com.rpa.manage.domain.dto.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Schema(description = "数据采集分页查询参数")
public class DataCollectionPageQuery {

    @Schema(description = "任务ID", example = "7")
    private Long taskId;

    @Schema(description = "关键字，支持按纳税人识别号或企业名称模糊查询", example = "重庆某某科技有限公司")
    private String keyword;

    @Schema(description = "采集状态：0-采集中，1-成功，2-失败", example = "1")
    private Integer status;

    @Schema(description = "开始时间，格式：yyyy-MM-dd HH:mm:ss", example = "2026-03-26 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间，格式：yyyy-MM-dd HH:mm:ss", example = "2026-03-26 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "页码，从1开始", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;
}
