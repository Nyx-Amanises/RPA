package com.rpa.manage.service.impl.auth;

import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.common.util.SecurityUtils;
import com.rpa.manage.domain.dto.auth.LoginRequest;
import com.rpa.manage.domain.entity.SysRole;
import com.rpa.manage.domain.entity.SysUser;
import com.rpa.manage.domain.repository.SysUserRepository;
import com.rpa.manage.security.JwtTokenProvider;
import com.rpa.manage.security.LoginSessionService;
import com.rpa.manage.security.LoginUser;
import com.rpa.manage.security.PermissionService;
import com.rpa.manage.service.auth.AuthService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类。
 *
 * <p>这个类主要处理两件事：
 * 1. 用户登录，校验账号密码并签发 token；
 * 2. 返回当前登录用户的资料、角色、权限和可访问路径。
 *
 * <p>它本身不直接做底层权限计算，而是把这部分工作交给 {@link PermissionService}。
 * 因此你可以把它理解成“认证入口 + 登录态返回数据组装器”。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /*
     * 下面这些依赖分别负责：
     * - 查询用户；
     * - 校验密码；
     * - 生成 JWT；
     * - 保存登录会话；
     * - 计算角色/权限/路径。
     */
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSessionService loginSessionService;
    private final PermissionService permissionService;

    /**
     * 处理登录请求。
     *
     * <p>主要流程如下：
     * 1. 按用户名查询用户；
     * 2. 校验密码和账号状态；
     * 3. 计算角色、权限、前端路径；
     * 4. 生成 token 并保存登录会话；
     * 5. 更新最后登录时间，返回给前端需要的登录结果。
     *
     * @param request 登录请求参数
     * @return 包含 token、token 前缀和用户信息的结果
     */
    @Override
    public Map<String, Object> login(LoginRequest request) {
        log.info("User login start, username={}", request.getUsername());
        SysUser user = sysUserRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException("账号已禁用，无法登录");
        }

        // 登录成功后，立刻把角色、权限、路由都算出来，给前端初始化菜单和按钮控制使用。
        List<SysRole> roles = loadRoles(user);
        List<String> permissions = permissionService.loadPermissions(user);
        List<String> menuPermissions = permissionService.loadMenuPermissions(user);
        List<String> buttonPermissions = permissionService.loadButtonPermissions(user);
        List<String> paths = permissionService.loadPaths(user);
        // 这里仍然保留“主角色”概念，主要是为了兼容 token 里原有的角色字段结构。
        SysRole primaryRole = roles.isEmpty() ? null : roles.get(0);
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .roleId(primaryRole == null ? null : primaryRole.getId())
                .roleCode(primaryRole == null ? null : primaryRole.getRoleCode())
                .roleName(primaryRole == null ? null : primaryRole.getRoleName())
                .username(user.getUsername())
                .password(user.getPassword())
                .status(user.getStatus())
                .permissions(permissions)
                .build();

        String token = jwtTokenProvider.generateToken(loginUser);
        loginSessionService.save(token, loginUser);
        // 最后登录时间是数据库中的业务字段，不放在 token 中，而是在登录成功时持久化。
        user.setLastLoginTime(LocalDateTime.now());
        sysUserRepository.save(user);
        log.info("User login success, userId={}, username={}", user.getId(), user.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("tokenHead", "Bearer ");
        result.put("userInfo", buildLoginUserInfo(user, roles, permissions, menuPermissions, buttonPermissions, paths));
        return result;
    }

    /**
     * 查询当前登录用户信息。
     *
     * <p>这里会重新查数据库，而不是直接完全相信 SecurityContext 里的缓存对象，
     * 目的是拿到用户最新资料，例如头像、手机号、状态等字段。
     *
     * @return 当前登录用户的完整资料载荷
     */
    @Override
    public Map<String, Object> currentUser() {
        LoginUser loginUser = SecurityUtils.currentUser();
        log.debug("Query current user, userId={}", loginUser.getUserId());
        SysUser user = sysUserRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new BusinessException("当前登录用户不存在"));
        List<SysRole> roles = loadRoles(user);
        List<String> permissions = permissionService.loadPermissions(user);
        List<String> menuPermissions = permissionService.loadMenuPermissions(user);
        List<String> buttonPermissions = permissionService.loadButtonPermissions(user);
        List<String> paths = permissionService.loadPaths(user);
        return buildCurrentUserPayload(user, roles, permissions, menuPermissions, buttonPermissions, paths);
    }

    /**
     * 组装登录接口里的 `userInfo` 结构。
     *
     * <p>登录成功时前端通常只需要基础资料、角色名、权限和路径，
     * 因此这里返回的字段比 `current-user` 接口更精简。
     *
     * @param user 用户实体
     * @param roles 用户角色列表
     * @param permissions 用户权限列表
     * @param paths 用户可访问路径列表
     * @return 登录接口中的用户信息结构
     */
    private Map<String, Object> buildLoginUserInfo(
            SysUser user,
            List<SysRole> roles,
            List<String> permissions,
            List<String> menuPermissions,
            List<String> buttonPermissions,
            List<String> paths
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("roleNames", roles.stream().map(SysRole::getRoleName).distinct().toList());
        result.put("permissions", permissions);
        result.put("menuPermissions", menuPermissions);
        result.put("buttonPermissions", buttonPermissions);
        result.put("paths", paths);
        return result;
    }

    /**
     * 组装当前用户接口返回的数据结构。
     *
     * <p>相比登录接口，这里会多返回邮箱、手机号、头像、状态、创建时间等更完整的数据，
     * 方便前端做个人中心展示。
     *
     * @param user 用户实体
     * @param roles 用户角色列表
     * @param permissions 用户权限列表
     * @param paths 用户可访问路径列表
     * @return 当前用户详情结构
     */
    private Map<String, Object> buildCurrentUserPayload(
            SysUser user,
            List<SysRole> roles,
            List<String> permissions,
            List<String> menuPermissions,
            List<String> buttonPermissions,
            List<String> paths
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("email", user.getEmail());
        result.put("mobile", user.getMobile());
        result.put("roleNames", roles.stream().map(SysRole::getRoleName).distinct().toList());
        result.put("permissions", permissions);
        result.put("menuPermissions", menuPermissions);
        result.put("buttonPermissions", buttonPermissions);
        result.put("paths", paths);
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("status", user.getStatus());
        result.put("createTime", user.getCreateTime());
        result.put("updateTime", user.getUpdateTime());
        return result;
    }

    /**
     * 统一读取用户有效角色。
     *
     * <p>虽然这里只是简单转调，但单独抽出来后，登录和当前用户查询的逻辑会更整齐，
     * 后续如果角色读取规则有变化，也更容易集中修改。
     *
     * @param user 用户实体
     * @return 有效角色列表
     */
    private List<SysRole> loadRoles(SysUser user) {
        return permissionService.loadEffectiveRoles(user);
    }
}
