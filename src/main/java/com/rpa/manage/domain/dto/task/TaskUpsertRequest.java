package com.rpa.manage.domain.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "任务新增/编辑请求参数")
public class TaskUpsertRequest {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100个字符")
    @Schema(description = "任务名称", example = "示例税务采集任务", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskName;

    @NotBlank(message = "纳税人识别号不能为空")
    @Size(max = 30, message = "纳税人识别号长度不能超过30个字符")
    @Schema(description = "纳税人识别号", example = "91500000MA5U123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taxpayerIdNo;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 120, message = "企业名称长度不能超过120个字符")
    @Schema(description = "企业名称", example = "重庆某某科技有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    private String enterpriseName;

    @NotNull(message = "流程ID不能为空")
    @Schema(description = "绑定流程ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long processId;

    @NotNull(message = "机器人ID不能为空")
    @Schema(description = "绑定机器人ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long robotId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "测试任务")
    private String remark;
}
