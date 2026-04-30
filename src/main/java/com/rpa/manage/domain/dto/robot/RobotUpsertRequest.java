package com.rpa.manage.domain.dto.robot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "机器人新增/编辑请求参数")
public class RobotUpsertRequest {

    @NotBlank(message = "机器人编码不能为空")
    @Size(max = 50, message = "机器人编码长度不能超过50个字符")
    @Schema(description = "机器人编码", example = "ROBOT_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String robotCode;

    @NotBlank(message = "机器人名称不能为空")
    @Size(max = 100, message = "机器人名称长度不能超过100个字符")
    @Schema(description = "机器人名称", example = "机器人A001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String robotName;

    @NotBlank(message = "机器人类型不能为空")
    @Size(max = 50, message = "机器人类型长度不能超过50个字符")
    @Schema(description = "机器人类型", example = "测试机器人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String robotType;

    @Size(max = 255, message = "描述长度不能超过255个字符")
    @Schema(description = "机器人描述", example = "用于联调测试的机器人")
    private String description;

    @NotNull(message = "机器人状态不能为空")
    @Schema(description = "机器人状态：0离线，1在线。工作中状态由任务执行时自动维护，不允许在新增/编辑时直接传入", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
