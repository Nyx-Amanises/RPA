package com.rpa.manage.domain.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@Schema(description = "AI Agent 额度计算响应")
public class AgentQuotaCalculateResponse {

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "Agent 执行步骤")
    private List<String> steps;

    @Schema(description = "风险或提示")
    private List<String> warnings;

    @Schema(description = "本次自动计算的指标结果")
    private List<Map<String, Object>> calculatedIndicators;

    @Schema(description = "额度计算结果")
    private Map<String, Object> quotaResult;

    @Schema(description = "解释文本")
    private String explanation;

    @Schema(description = "是否调用外部模型生成解释")
    private Boolean modelUsed;

    @Schema(description = "使用的模型")
    private String model;
}
