package com.rpa.manage.domain.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "流程分页查询参数")
public class ProcessPageQuery {

    @Schema(description = "流程名称，支持模糊查询", example = "发票采集流程")
    private String processName;

    @Schema(description = "流程编码，支持模糊查询", example = "PROCESS_001")
    private String processCode;

    @Schema(description = "流程状态：0禁用，1启用", example = "1")
    private Integer status;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
