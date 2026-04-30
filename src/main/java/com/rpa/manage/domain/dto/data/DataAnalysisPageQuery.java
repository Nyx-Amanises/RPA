package com.rpa.manage.domain.dto.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Schema(description = "数据解析分页查询参数")
public class DataAnalysisPageQuery {

    @Schema(description = "任务ID", example = "7")
    private Long taskId;

    @Schema(description = "解析状态：0-解析中，1-成功，2-失败", example = "1")
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
