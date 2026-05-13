package com.rpa.manage.domain.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI Agent 辅助响应")
public class AgentAssistResponse {

    @Schema(description = "响应内容")
    private String content;

    @Schema(description = "是否使用本地规则生成")
    private Boolean localFallback;

    @Schema(description = "使用的模型")
    private String model;
}
