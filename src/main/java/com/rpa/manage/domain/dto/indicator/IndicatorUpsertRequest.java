package com.rpa.manage.domain.dto.indicator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "指标新增/编辑请求参数")
public class IndicatorUpsertRequest {

    @NotBlank(message = "指标名称不能为空")
    @Size(max = 100, message = "指标名称长度不能超过100个字符")
    @Schema(description = "指标名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicatorName;

    @NotBlank(message = "指标编码不能为空")
    @Size(max = 50, message = "指标编码长度不能超过50个字符")
    @Schema(description = "指标编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicatorCode;

    @Size(max = 50, message = "结果变量名长度不能超过50个字符")
    @Schema(description = "指标结果变量名，如 taxRate")
    private String resultVarName;

    @NotBlank(message = "指标逻辑说明不能为空")
    @Size(max = 500, message = "指标逻辑说明长度不能超过500个字符")
    @Schema(description = "展示用指标逻辑说明", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicatorLogic;

    @NotBlank(message = "指标计算公式不能为空")
    @Schema(description = "后端可执行的受限指标公式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String formulaExpression;

    @NotNull(message = "数据来源任务ID不能为空")
    @Schema(description = "数据来源任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @Schema(description = "状态：1启用，0停用")
    private Integer status = 1;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注")
    private String remark;
}
