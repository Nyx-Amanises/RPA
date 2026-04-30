package com.rpa.manage.domain.dto.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Schema(description = "业务数据分页查询参数")
public class BusinessDataPageQuery {

    @Schema(description = "关键字，支持按纳税人识别号或企业名称模糊查询", example = "91500000MA5U123456")
    private String keyword;

    @Schema(description = "任务ID", example = "7")
    private Long taskId;

    @Schema(description = "税区ID", example = "500100")
    private String taxAreaId;

    @Schema(description = "数据状态：1-可用，其他状态按业务扩展", example = "1")
    private Integer dataStatus;

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
