package com.rpa.manage.domain.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 模型配置响应")
public class AiModelConfigResponse {

    @Schema(description = "模型供应商")
    private String provider;

    @Schema(description = "模型接口地址")
    private String baseUrl;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "是否已保存 API Key")
    private Boolean apiKeySaved;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
