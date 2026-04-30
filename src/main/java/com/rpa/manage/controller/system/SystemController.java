package com.rpa.manage.controller.system;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.system.ChangePasswordRequest;
import com.rpa.manage.domain.dto.system.ProfileUpdateRequest;
import com.rpa.manage.domain.dto.system.ResourceUpsertRequest;
import com.rpa.manage.domain.dto.system.RolePageQuery;
import com.rpa.manage.domain.dto.system.RoleResourceAssignRequest;
import com.rpa.manage.domain.dto.system.RoleUpsertRequest;
import com.rpa.manage.domain.dto.system.UserPageQuery;
import com.rpa.manage.domain.dto.system.UserStatusRequest;
import com.rpa.manage.domain.dto.system.UserUpsertRequest;
import com.rpa.manage.service.system.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统管理模块控制器。
 *
 * <p>这个类可以看成“后台管理核心入口”，主要对外暴露四类接口：
 * 1. 当前登录用户的个人资料维护；
 * 2. 系统用户管理；
 * 3. 角色管理与资源分配；
 * 4. 资源树和资源维护。
 *
 * <p>Controller 层本身不直接写业务逻辑，主要负责接收请求、触发参数校验、
 * 调用 {@link SystemService}，最后把结果包装成统一的 {@code Result} 返回给前端。
 */
@Tag(name = "系统管理", description = "个人信息、用户、角色、资源相关接口")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    /*
     * ==================== 个人中心 ====================
     * 这一组接口只处理“当前登录用户自己”的资料和密码。
     */

    /**
     * 更新当前登录用户的个人资料。
     *
     * @param request 个人资料修改请求
     * @return 更新后的用户资料
     */
    @Operation(summary = "更新个人信息", description = "更新当前登录用户的姓名、邮箱和手机号。")
    @PutMapping("/api/v1/profile")
    public Result<Map<String, Object>> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.success(systemService.updateProfile(request));
    }

    /**
     * 上传当前登录用户头像。
     *
     * <p>前端会以 multipart/form-data 的形式上传文件，
     * 服务层保存文件后返回头像访问地址。
     *
     * @param file 头像文件
     * @return 上传结果，包含头像 URL 等信息
     */
    @Operation(summary = "上传头像", description = "上传当前登录用户头像，文件会保存到本地 uploads 目录，并返回可直接访问的头像地址。")
    @PostMapping(value = "/api/v1/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAvatar(
            @Parameter(description = "头像文件，支持 jpg、jpeg、png、gif、webp")
            @RequestParam("file") MultipartFile file
    ) {
        return Result.success(systemService.uploadAvatar(file));
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param request 修改密码请求
     * @return 空结果或成功标记
     */
    @Operation(summary = "修改个人密码", description = "校验原密码后修改当前登录用户密码。")
    @PutMapping("/api/v1/profile/password")
    public Result<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return Result.success(systemService.changePassword(request));
    }

    /*
     * ==================== 用户管理 ====================
     * 这一组接口面向管理员，用来维护系统用户。
     */

    /**
     * 分页查询用户列表。
     *
     * @param query 查询条件
     * @return 用户分页结果
     */
    @Operation(summary = "用户分页查询", description = "分页查询用户列表，支持用户名、姓名和角色过滤。所需权限：system:user:page")
    @GetMapping("/api/v1/users/page")
    @PreAuthorize("hasAuthority('system:user:page')")
    public Result<PageResult<Map<String, Object>>> usersPage(UserPageQuery query) {
        return Result.success(systemService.usersPage(query));
    }

    /**
     * 新增系统用户。
     *
     * @param request 用户新增请求
     * @return 新建后的用户信息
     */
    @Operation(summary = "新增用户", description = "新增系统用户，并同时建立用户与角色的关联关系。所需权限：system:user:create")
    @PostMapping("/api/v1/users")
    @PreAuthorize("hasAuthority('system:user:create')")
    public Result<Map<String, Object>> createUser(@Valid @RequestBody UserUpsertRequest request) {
        return Result.success(systemService.createUser(request));
    }

    /**
     * 修改指定用户。
     *
     * @param id 用户 ID
     * @param request 用户修改请求
     * @return 修改后的用户信息
     */
    @Operation(summary = "修改用户", description = "修改用户基础资料、状态和角色列表。所需权限：system:user:update")
    @PutMapping("/api/v1/users/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<Map<String, Object>> updateUser(
            @Parameter(description = "用户 ID", example = "3") @PathVariable Long id,
            @Valid @RequestBody UserUpsertRequest request
    ) {
        return Result.success(systemService.updateUser(id, request));
    }

    /**
     * 将指定用户密码重置为系统默认密码。
     *
     * @param id 用户 ID
     * @return 重置结果
     */
    @Operation(summary = "重置用户密码", description = "将指定用户密码重置为系统默认密码。所需权限：system:user:reset-password")
    @PutMapping("/api/v1/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:reset-password')")
    public Result<Map<String, Object>> resetPassword(
            @Parameter(description = "用户 ID", example = "3") @PathVariable Long id
    ) {
        return Result.success(systemService.resetPassword(id));
    }

    /**
     * 启用或禁用指定用户。
     *
     * @param id 用户 ID
     * @param request 状态修改请求
     * @return 最新状态结果
     */
    @Operation(summary = "修改用户状态", description = "启用或禁用指定用户。所需权限：system:user:status")
    @PutMapping("/api/v1/users/{id}/status")
    @PreAuthorize("hasAuthority('system:user:status')")
    public Result<Map<String, Object>> changeStatus(
            @Parameter(description = "用户 ID", example = "3") @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return Result.success(systemService.changeUserStatus(id, request));
    }

    /**
     * 删除指定用户。
     *
     * @param id 用户 ID
     * @return 删除结果
     */
    @Operation(summary = "删除用户", description = "删除指定用户及其角色关联。所需权限：system:user:delete")
    @DeleteMapping("/api/v1/users/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<Map<String, Object>> deleteUser(
            @Parameter(description = "用户 ID", example = "3") @PathVariable Long id
    ) {
        return Result.success(systemService.deleteUser(id));
    }

    /*
     * ==================== 角色管理 ====================
     * 这一组接口负责角色本身，以及角色和资源之间的分配关系。
     */

    /**
     * 分页查询角色列表。
     *
     * @param query 查询条件
     * @return 角色分页结果
     */
    @Operation(summary = "角色分页查询", description = "分页查询角色列表，返回角色基础信息、权限摘要和用户数。所需权限：system:role:page")
    @GetMapping("/api/v1/roles/page")
    @PreAuthorize("hasAuthority('system:role:page')")
    public Result<PageResult<Map<String, Object>>> rolesPage(RolePageQuery query) {
        return Result.success(systemService.rolesPage(query));
    }

    /**
     * 新增角色。
     *
     * @param request 角色新增请求
     * @return 新建后的角色信息
     */
    @Operation(summary = "新增角色", description = "新增系统角色，角色编码创建后作为稳定标识使用。所需权限：system:role:create")
    @PostMapping("/api/v1/roles")
    @PreAuthorize("hasAuthority('system:role:create')")
    public Result<Map<String, Object>> createRole(@Valid @RequestBody RoleUpsertRequest request) {
        return Result.success(systemService.createRole(request));
    }

    /**
     * 修改指定角色。
     *
     * @param id 角色 ID
     * @param request 角色修改请求
     * @return 修改后的角色信息
     */
    @Operation(summary = "修改角色", description = "修改角色名称、描述、备注和状态，角色编码不可修改。所需权限：system:role:update")
    @PutMapping("/api/v1/roles/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    public Result<Map<String, Object>> updateRole(
            @Parameter(description = "角色 ID", example = "14") @PathVariable Long id,
            @Valid @RequestBody RoleUpsertRequest request
    ) {
        return Result.success(systemService.updateRole(id, request));
    }

    /**
     * 查询角色当前已分配的资源 ID。
     *
     * @param id 角色 ID
     * @return 资源 ID 列表
     */
    @Operation(summary = "查询角色已分配资源", description = "返回当前角色已分配的资源 ID 列表，供前端权限树回显。所需权限：system:role:assign-resource")
    @GetMapping("/api/v1/roles/{id}/resources")
    @PreAuthorize("hasAuthority('system:role:assign-resource')")
    public Result<List<Long>> roleResources(
            @Parameter(description = "角色 ID", example = "14") @PathVariable Long id
    ) {
        return Result.success(systemService.roleResources(id));
    }

    /**
     * 覆盖式分配角色资源。
     *
     * @param id 角色 ID
     * @param request 角色资源分配请求
     * @return 分配结果
     */
    @Operation(summary = "分配角色资源", description = "覆盖式分配角色权限，旧资源关联会先清空再写入新集合。所需权限：system:role:assign-resource")
    @PutMapping("/api/v1/roles/{id}/resources")
    @PreAuthorize("hasAuthority('system:role:assign-resource')")
    public Result<Map<String, Object>> assignRoleResources(
            @Parameter(description = "角色 ID", example = "14") @PathVariable Long id,
            @Valid @RequestBody RoleResourceAssignRequest request
    ) {
        return Result.success(systemService.assignRoleResources(id, request));
    }

    /**
     * 删除指定角色。
     *
     * @param id 角色 ID
     * @return 删除结果
     */
    @Operation(summary = "删除角色", description = "删除指定角色；若角色下仍有用户，则不允许删除。所需权限：system:role:delete")
    @DeleteMapping("/api/v1/roles/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Map<String, Object>> deleteRole(
            @Parameter(description = "角色 ID", example = "14") @PathVariable Long id
    ) {
        return Result.success(systemService.deleteRole(id));
    }

    /*
     * ==================== 资源管理 ====================
     * 这一组接口主要维护目录、菜单、按钮等资源数据。
     */

    /**
     * 查询资源树。
     *
     * @param resourceType 资源类型过滤条件
     * @param resourceName 资源名称模糊搜索条件
     * @return 资源树结果
     */
    @Operation(summary = "查询资源树", description = "返回资源树结构，可按资源类型过滤，供菜单展示和权限分配使用。所需权限：system:resource:tree")
    @GetMapping("/api/v1/resources/tree")
    @PreAuthorize("hasAuthority('system:resource:tree')")
    public Result<List<Map<String, Object>>> resourceTree(
            @Parameter(description = "资源类型过滤，1 目录，2 菜单，3 按钮", example = "2")
            @RequestParam(required = false) Integer resourceType,
            @Parameter(description = "资源名称，支持模糊查询", example = "用户")
            @RequestParam(required = false) String resourceName
    ) {
        return Result.success(systemService.resourceTree(resourceType, resourceName));
    }

    /**
     * 新增资源。
     *
     * @param request 资源新增请求
     * @return 新建后的资源信息
     */
    @Operation(summary = "新增资源", description = "新增目录、菜单或按钮资源。所需权限：system:resource:create")
    @PostMapping("/api/v1/resources")
    @PreAuthorize("hasAuthority('system:resource:create')")
    public Result<Map<String, Object>> createResource(@Valid @RequestBody ResourceUpsertRequest request) {
        return Result.success(systemService.createResource(request));
    }

    /**
     * 修改指定资源。
     *
     * @param id 资源 ID
     * @param request 资源修改请求
     * @return 修改后的资源信息
     */
    @Operation(summary = "修改资源", description = "修改资源基础信息、路由信息、权限标识和状态。所需权限：system:resource:update")
    @PutMapping("/api/v1/resources/{id}")
    @PreAuthorize("hasAuthority('system:resource:update')")
    public Result<Map<String, Object>> updateResource(
            @Parameter(description = "资源 ID", example = "28") @PathVariable Long id,
            @Valid @RequestBody ResourceUpsertRequest request
    ) {
        return Result.success(systemService.updateResource(id, request));
    }

    /**
     * 删除指定资源。
     *
     * @param id 资源 ID
     * @return 删除结果
     */
    @Operation(summary = "删除资源", description = "删除指定资源；若存在子资源或已被角色引用，则不允许删除。所需权限：system:resource:delete")
    @DeleteMapping("/api/v1/resources/{id}")
    @PreAuthorize("hasAuthority('system:resource:delete')")
    public Result<Map<String, Object>> deleteResource(
            @Parameter(description = "资源 ID", example = "28") @PathVariable Long id
    ) {
        return Result.success(systemService.deleteResource(id));
    }
}
