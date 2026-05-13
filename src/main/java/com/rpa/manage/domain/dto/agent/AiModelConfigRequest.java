package com.rpa.manage.domain.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 模型配置保存请求")
public class AiModelConfigRequest {

    @Schema(description = "模型供应商：openai/deepseek/doubao/doubao-responses/custom")
    @Size(max = 40, message = "供应商不能超过40个字符")
    private String provider;

    @Schema(description = "模型接口地址，支持 Chat Completions 或 Responses")
    @Size(max = 500, message = "接口地址不能超过500个字符")
    private String baseUrl;

    @Schema(description = "模型名称")
    @Size(max = 120, message = "模型名称不能超过120个字符")
    private String model;

    @Schema(description = "API Key；为空时保留原值")
    @Size(max = 1000, message = "API Key 不能超过1000个字符")
    private String apiKey;

    @Schema(description = "是否清空已保存 API Key")
    private Boolean clearApiKey;
}
