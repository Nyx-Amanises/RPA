package com.rpa.manage.domain.dto.indicator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "额度计算触发请求参数")
public class QuotaCalculateRequest {

    @Schema(description = "指定同批次执行记录ID，优先使用该批次指标结果")
    private Long executionId;

    @Schema(description = "纳税人识别号；为空时自动取最近指标结果所属企业")
    private String taxpayerIdNo;

    @Schema(description = "企业名称；纳税人识别号为空时可用企业名称筛选")
    private String enterpriseName;
}
