package com.rpa.manage.domain.dto.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "流程新增/编辑请求参数")
public class ProcessUpsertRequest {

    @NotBlank(message = "流程编码不能为空")
    @Size(max = 50, message = "流程编码长度不能超过50个字符")
    @Schema(description = "流程编码。新增时必填，创建后不允许修改", example = "PROCESS_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processCode;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称长度不能超过100个字符")
    @Schema(description = "流程名称", example = "发票采集流程", requiredMode = Schema.RequiredMode.REQUIRED)
    private String processName;

    @Size(max = 255, message = "流程描述长度不能超过255个字符")
    @Schema(description = "流程描述", example = "用于采集和解析发票数据")
    private String description;

    @NotNull(message = "流程状态不能为空")
    @Schema(description = "流程状态：0禁用，1启用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
