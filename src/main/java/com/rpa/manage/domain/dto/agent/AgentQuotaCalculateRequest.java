package com.rpa.manage.domain.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI Agent 额度计算请求")
public class AgentQuotaCalculateRequest {

    @Schema(description = "额度计算规则ID")
    @NotNull(message = "额度计算规则不能为空")
    private Long quotaRuleId;

    @Schema(description = "指定执行记录ID；为空时自动使用规则可匹配的最近指标结果")
    private Long executionId;

    @Schema(description = "纳税人识别号")
    @Size(max = 32, message = "纳税人识别号不能超过32个字符")
    private String taxpayerIdNo;

    @Schema(description = "企业名称")
    @Size(max = 120, message = "企业名称不能超过120个字符")
    private String enterpriseName;

    @Schema(description = "是否自动补算规则关联指标")
    private Boolean autoCalculateMissingIndicators = Boolean.TRUE;

    @Schema(description = "模型供应商：openai/deepseek/doubao/doubao-responses/custom")
    private String provider;

    @Schema(description = "模型接口地址，支持 Chat Completions 或 Responses")
    private String baseUrl;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "API Key")
    private String apiKey;
}
