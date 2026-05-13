package com.rpa.manage.domain.dto.indicator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "指标额度计算新增/编辑请求参数")
public class QuotaRuleUpsertRequest {

    @NotBlank(message = "额度名称不能为空")
    @Size(max = 100, message = "额度名称长度不能超过100个字符")
    @Schema(description = "额度名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String quotaName;

    @NotBlank(message = "结果变量名称不能为空")
    @Size(max = 50, message = "结果变量名称长度不能超过50个字符")
    @Schema(description = "额度结果变量名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resultVarName;

    @NotBlank(message = "输出数据模板不能为空")
    @Schema(description = "输出数据模板，支持 ${变量名}", requiredMode = Schema.RequiredMode.REQUIRED)
    private String outputTemplate;

    @NotEmpty(message = "至少选择一个指标")
    @Schema(description = "关联指标ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> indicatorIds = new ArrayList<>();

    @NotEmpty(message = "至少配置一个额度计算分支")
    @Valid
    @Schema(description = "额度计算分支列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<QuotaRuleBranchRequest> branches = new ArrayList<>();

    @Schema(description = "状态：1启用，0停用")
    private Integer status = 1;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注")
    private String remark;
}
