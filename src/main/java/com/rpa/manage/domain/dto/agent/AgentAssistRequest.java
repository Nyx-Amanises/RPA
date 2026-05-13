package com.rpa.manage.domain.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI Agent 辅助请求")
public class AgentAssistRequest {

    @Schema(description = "任务类型：FORMULA_DRAFT/FORMULA_CHECK/QUOTA_EXPLAIN")
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @Schema(description = "模型供应商：openai/deepseek/doubao/doubao-responses/custom")
    private String provider;

    @Schema(description = "模型接口地址，支持 Chat Completions 或 Responses")
    private String baseUrl;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "用户输入")
    @NotBlank(message = "用户输入不能为空")
    @Size(max = 8000, message = "用户输入不能超过8000个字符")
    private String userInput;

    @Schema(description = "业务上下文，如已选指标、额度规则、计算结果")
    @Size(max = 12000, message = "业务上下文不能超过12000个字符")
    private String context;
}
