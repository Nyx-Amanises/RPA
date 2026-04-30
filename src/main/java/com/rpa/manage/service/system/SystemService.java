package com.rpa.manage.service.system;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.domain.dto.system.ChangePasswordRequest;
import com.rpa.manage.domain.dto.system.ProfileUpdateRequest;
import com.rpa.manage.domain.dto.system.ResourceUpsertRequest;
import com.rpa.manage.domain.dto.system.RolePageQuery;
import com.rpa.manage.domain.dto.system.RoleResourceAssignRequest;
import com.rpa.manage.domain.dto.system.RoleUpsertRequest;
import com.rpa.manage.domain.dto.system.UserPageQuery;
import com.rpa.manage.domain.dto.system.UserStatusRequest;
import com.rpa.manage.domain.dto.system.UserUpsertRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统管理模块服务接口。
 *
 * <p>这里定义的是系统管理模块对控制层暴露出来的能力边界。
 * Controller 只依赖这个接口，不直接依赖实现类，这样职责会更清晰，也方便后续替换实现。
 */
public interface SystemService {

    /**
     * 更新当前登录用户的个人资料。
     *
     * @param request 个人资料修改请求
     * @return 更新后的个人资料
     */
    Map<String, Object> updateProfile(ProfileUpdateRequest request);

    /**
     * 上传当前登录用户头像。
     *
     * @param file 头像文件
     * @return 上传结果
     */
    Map<String, Object> uploadAvatar(MultipartFile file);

    /**
     * 修改当前登录用户密码。
     *
     * @param request 修改密码请求
     * @return 修改结果
     */
    Map<String, Object> changePassword(ChangePasswordRequest request);

    /**
     * 分页查询用户列表。
     *
     * @param query 查询条件
     * @return 用户分页结果
     */
    PageResult<Map<String, Object>> usersPage(UserPageQuery query);

    /**
     * 新增用户。
     *
     * @param request 用户新增请求
     * @return 新建后的用户信息
     */
    Map<String, Object> createUser(UserUpsertRequest request);

    /**
     * 修改指定用户。
     *
     * @param id 用户 ID
     * @param request 用户修改请求
     * @return 修改后的用户信息
     */
    Map<String, Object> updateUser(Long id, UserUpsertRequest request);

    /**
     * 重置指定用户密码。
     *
     * @param id 用户 ID
     * @return 重置结果
     */
    Map<String, Object> resetPassword(Long id);

    /**
     * 修改用户状态。
     *
     * @param id 用户 ID
     * @param request 状态修改请求
     * @return 修改结果
     */
    Map<String, Object> changeUserStatus(Long id, UserStatusRequest request);

    /**
     * 删除指定用户。
     *
     * @param id 用户 ID
     * @return 删除结果
     */
    Map<String, Object> deleteUser(Long id);

    /**
     * 分页查询角色列表。
     *
     * @param query 查询条件
     * @return 角色分页结果
     */
    PageResult<Map<String, Object>> rolesPage(RolePageQuery query);

    /**
     * 新增角色。
     *
     * @param request 角色新增请求
     * @return 新建后的角色信息
     */
    Map<String, Object> createRole(RoleUpsertRequest request);

    /**
     * 修改指定角色。
     *
     * @param id 角色 ID
     * @param request 角色修改请求
     * @return 修改后的角色信息
     */
    Map<String, Object> updateRole(Long id, RoleUpsertRequest request);

    /**
     * 查询角色当前已分配的资源 ID 列表。
     *
     * @param id 角色 ID
     * @return 资源 ID 列表
     */
    List<Long> roleResources(Long id);

    /**
     * 分配角色资源。
     *
     * @param id 角色 ID
     * @param request 资源分配请求
     * @return 分配结果
     */
    Map<String, Object> assignRoleResources(Long id, RoleResourceAssignRequest request);

    /**
     * 删除指定角色。
     *
     * @param id 角色 ID
     * @return 删除结果
     */
    Map<String, Object> deleteRole(Long id);

    /**
     * 查询资源树。
     *
     * @param resourceType 资源类型过滤条件
     * @param resourceName 资源名称过滤条件
     * @return 资源树
     */
    List<Map<String, Object>> resourceTree(Integer resourceType, String resourceName);

    /**
     * 新增资源。
     *
     * @param request 资源新增请求
     * @return 新建后的资源信息
     */
    Map<String, Object> createResource(ResourceUpsertRequest request);

    /**
     * 修改指定资源。
     *
     * @param id 资源 ID
     * @param request 资源修改请求
     * @return 修改后的资源信息
     */
    Map<String, Object> updateResource(Long id, ResourceUpsertRequest request);

    /**
     * 删除指定资源。
     *
     * @param id 资源 ID
     * @return 删除结果
     */
    Map<String, Object> deleteResource(Long id);
}
