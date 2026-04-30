package com.rpa.manage.domain.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Schema(description = "任务分页查询参数")
public class TaskPageQuery {

    @Schema(description = "任务编码或任务名称，支持模糊查询", example = "TASK_20260325")
    private String keyword;

    @Schema(description = "任务状态：0待执行，1执行中，2执行成功，3执行失败", example = "0")
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间开始", example = "2026-03-25 00:00:00")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间结束", example = "2026-03-25 23:59:59")
    private LocalDateTime endTime;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
