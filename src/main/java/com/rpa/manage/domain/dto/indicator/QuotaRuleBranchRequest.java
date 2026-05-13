package com.rpa.manage.domain.dto.indicator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "额度计算分支请求参数")
public class QuotaRuleBranchRequest {

    @Size(max = 100, message = "分支名称长度不能超过100个字符")
    @Schema(description = "分支名称")
    private String branchName;

    @Schema(description = "判断条件表达式，默认分支可为空或 else")
    private String conditionExpression;

    @NotBlank(message = "额度计算公式不能为空")
    @Schema(description = "该分支对应的额度计算公式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String calculationExpression;

    @NotNull(message = "分支顺序不能为空")
    @Schema(description = "分支顺序，从小到大", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer branchOrder;

    @Schema(description = "是否默认分支")
    private Boolean defaultBranch = false;

    @Schema(description = "状态：1启用，0停用")
    private Integer status = 1;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注")
    private String remark;
}
