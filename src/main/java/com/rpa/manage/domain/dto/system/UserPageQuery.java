package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分页查询参数。
 */
@Data
@Schema(description = "用户分页查询参数")
public class UserPageQuery {

    @Schema(description = "用户名，支持模糊查询", example = "admin")
    private String username;

    @Schema(description = "姓名，支持模糊查询", example = "系统管理员")
    private String realName;

    @Schema(description = "角色 ID", example = "14")
    private Long roleId;

    @Schema(description = "页码", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;
}
