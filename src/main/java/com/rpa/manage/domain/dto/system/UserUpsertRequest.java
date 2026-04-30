package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 用户新增/修改请求。
 */
@Data
@Schema(description = "用户新增或修改请求")
public class UserUpsertRequest {

    @Schema(description = "用户名", example = "operator02")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "真实姓名", example = "操作员乙")
    @NotBlank(message = "姓名不能为空")
    private String realName;

    @Schema(description = "邮箱", example = "operator02@rpa.com")
    private String email;

    @Schema(description = "手机号", example = "13800138003")
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @ArraySchema(schema = @Schema(description = "角色 ID", example = "14"), arraySchema = @Schema(description = "角色 ID 列表"))
    @NotEmpty(message = "角色 ID 列表不能为空")
    private List<Long> roleIds;

    @Schema(description = "状态，1 启用，0 禁用", example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
