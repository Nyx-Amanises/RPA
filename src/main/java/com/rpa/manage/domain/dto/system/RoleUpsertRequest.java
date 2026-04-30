package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色新增/修改请求。
 */
@Data
@Schema(description = "角色新增或修改请求")
public class RoleUpsertRequest {

    @Schema(description = "角色编码。创建时必填，更新时必须保持不变", example = "ADMIN")
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "角色描述", example = "系统管理员角色")
    private String description;

    @Schema(description = "备注", example = "初始化角色")
    private String remark;

    @Schema(description = "状态，1 启用，0 禁用", example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
