package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色分页查询参数。
 */
@Data
@Schema(description = "角色分页查询参数")
public class RolePageQuery {

    @Schema(description = "角色名称，支持模糊查询", example = "管理员")
    private String roleName;

    @Schema(description = "角色编码，支持模糊查询", example = "ADMIN")
    private String roleCode;

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;
}
