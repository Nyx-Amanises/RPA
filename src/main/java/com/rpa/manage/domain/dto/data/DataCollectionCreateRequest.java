package com.rpa.manage.domain.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
public class DataCollectionCreateRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    @NotBlank(message = "纳税人识别号不能为空")
    private String taxpayerIdNo;
    @NotBlank(message = "企业名称不能为空")
    private String enterpriseName;
    @NotBlank(message = "数据来源不能为空")
    private String sourceName;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "原始数据不能为空")
    private JsonNode rawData;
}
