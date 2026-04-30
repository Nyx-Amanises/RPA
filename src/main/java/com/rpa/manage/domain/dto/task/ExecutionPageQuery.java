package com.rpa.manage.domain.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Schema(description = "执行记录分页查询参数")
public class ExecutionPageQuery {

    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    @Schema(description = "执行状态：1执行中，2成功，3失败，4排队中", example = "2")
    private Integer executeStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "执行开始时间", example = "2026-03-25 00:00:00")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "执行结束时间", example = "2026-03-25 23:59:59")
    private LocalDateTime endTime;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
