package com.rpa.manage.domain.dto.system;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "资源新增或修改请求")
public class ResourceUpsertRequest {

    @Schema(description = "父级资源 ID，根节点可为空", example = "28")
    private Long parentId;

    @Schema(description = "资源名称", example = "用户管理")
    @NotBlank(message = "资源名称不能为空")
    private String resourceName;

    @Schema(description = "资源编码", example = "USER_MANAGE")
    @NotBlank(message = "资源编码不能为空")
    private String resourceCode;

    @Schema(description = "资源类型，1 目录，2 菜单，3 按钮", example = "2")
    @NotNull(message = "资源类型不能为空")
    private Integer resourceType;

    @Schema(description = "访问路径", example = "/system/users")
    private String path;

    @Schema(description = "前端组件路径", example = "system/users/index")
    private String component;

    @Schema(description = "权限标识", example = "system:user:view")
    @JsonAlias("permissionCode")
    private String permissionKey;

    @Schema(description = "图标", example = "user")
    private String icon;

    @Schema(description = "排序号", example = "1")
    private Integer sortNo;

    @Schema(description = "备注", example = "用户管理菜单")
    private String remark;

    @Schema(description = "状态，1 启用，0 禁用", example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
