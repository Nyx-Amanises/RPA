package com.rpa.manage.domain.dto.indicator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "指标额度计算分页查询参数")
public class QuotaRulePageQuery {

    @Schema(description = "额度名称，支持模糊查询")
    private String keyword;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
