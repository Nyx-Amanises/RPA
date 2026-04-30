package com.rpa.manage.domain.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "流程设计保存请求参数")
public class ProcessDesignRequest {

    @Valid
    @NotEmpty(message = "流程步骤不能为空")
    @Schema(description = "流程步骤列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<StepItem> steps;

    @Data
    @Schema(description = "流程步骤项")
    public static class StepItem {

        @NotNull(message = "步骤序号不能为空")
        @Schema(description = "步骤序号，从1开始递增", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer stepNo;

        @NotBlank(message = "步骤名称不能为空")
        @Size(max = 100, message = "步骤名称长度不能超过100个字符")
        @Schema(description = "步骤名称", example = "登录税务系统", requiredMode = Schema.RequiredMode.REQUIRED)
        private String stepName;

        @NotBlank(message = "步骤类型不能为空")
        @Size(max = 50, message = "步骤类型长度不能超过50个字符")
        @Schema(description = "步骤类型", example = "ACTION", requiredMode = Schema.RequiredMode.REQUIRED)
        private String stepType;

        @NotBlank(message = "脚本语言不能为空")
        @Size(max = 30, message = "脚本语言长度不能超过30个字符")
        @Schema(description = "脚本语言", example = "Python", requiredMode = Schema.RequiredMode.REQUIRED)
        private String scriptLang;

        @NotBlank(message = "脚本内容不能为空")
        @Schema(description = "脚本内容", example = "print('hello')", requiredMode = Schema.RequiredMode.REQUIRED)
        private String scriptContent;

        @Min(value = 1, message = "步骤超时时间必须大于0秒")
        @Max(value = 3600, message = "步骤超时时间不能超过3600秒")
        @Schema(description = "步骤超时时间，单位秒", example = "60")
        private Integer timeoutSeconds;

        @Size(max = 20, message = "失败策略长度不能超过20个字符")
        @Schema(description = "失败策略，STOP/CONTINUE/RETRY", example = "STOP")
        private String failureStrategy;
    }
}
