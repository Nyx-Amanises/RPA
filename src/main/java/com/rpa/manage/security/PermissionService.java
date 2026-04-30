package com.rpa.manage.security;

import com.rpa.manage.domain.entity.SysRole;
import com.rpa.manage.domain.entity.SysResource;
import com.rpa.manage.domain.entity.SysRoleResource;
import com.rpa.manage.domain.entity.SysUser;
import com.rpa.manage.domain.entity.SysUserRole;
import com.rpa.manage.domain.repository.SysRoleRepository;
import com.rpa.manage.domain.repository.SysResourceRepository;
import com.rpa.manage.domain.repository.SysRoleResourceRepository;
import com.rpa.manage.domain.repository.SysUserRepository;
import com.rpa.manage.domain.repository.SysUserRoleRepository;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 权限装配服务。
 *
 * <p>这个类负责把“用户拥有哪些角色、角色绑定了哪些资源、这些资源最终对应哪些权限和路径”
 * 这一整条链路串起来。前端登录后看到的 `permissions` 和 `paths`，核心就是从这里算出来的。
 *
 * <p>如果你在排查权限问题，可以按下面的顺序理解：
 * 1. 先看用户最终有哪些有效角色；
 * 2. 再看这些角色直接分配了哪些资源；
 * 3. 最后看资源扩展、状态过滤之后，变成哪些权限标识和菜单路径。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    /**
     * 约定 1 表示启用状态。
     */
    private static final Integer ENABLED_STATUS = 1;
    /**
     * 约定 2 表示菜单资源。
     */
    private static final Integer MENU_TYPE = 2;
    /**
     * 绾﹀畾 3 琛ㄧず鎸夐挳璧勬簮銆?
     */
    private static final Integer BUTTON_TYPE = 3;
    private static final Set<String> ENTRY_PERMISSION_SUFFIXES = Set.of(":page", ":tree", ":overview");
    private static final String PERMISSION_CACHE_PREFIX = "rpa:permission:user:";
    /**
     * 个人中心是登录用户默认可访问的页面，因此这里固定补一条路径。
     */
    private static final String PROFILE_PATH = "/system/profile";

    /*
     * 这些 Repository/组件负责从数据库读取角色、资源、关系表数据，
     * PermissionService 自己不关心 SQL 细节，只负责把它们组织成最终权限结果。
     */
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleResourceRepository sysRoleResourceRepository;
    private final SysResourceRepository sysResourceRepository;
    private final ResourcePermissionResolver resourcePermissionResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.permission.cache-ttl-seconds:1800}")
    private long permissionCacheTtlSeconds;

    /**
     * 加载用户当前有效的角色列表。
     *
     * <p>这里的“有效”主要指两件事：
     * 1. 角色确实被分配给了这个用户；
     * 2. 角色状态必须是启用。
     *
     * @param user 当前用户实体
     * @return 按原始分配顺序整理好的有效角色列表
     */
    public List<SysRole> loadEffectiveRoles(SysUser user) {
        List<Long> roleIds = loadRoleIds(user);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> orderedRoleIds = roleIds;
        return sysRoleRepository.findAllById(orderedRoleIds).stream()
                .filter(role -> isEnabled(role.getStatus()))
                // Repository 查出来的顺序不一定和分配顺序一致，这里手动按原顺序排回去。
                .sorted((left, right) -> Integer.compare(
                        orderedRoleIds.indexOf(left.getId()),
                        orderedRoleIds.indexOf(right.getId())
                ))
                .toList();
    }

    /**
     * 计算用户最终拥有的权限标识列表。
     *
     * <p>这里返回的是按钮权限、页面权限等字符串，例如 `system:user:list`。
     * Spring Security 做接口鉴权时，拿到的就是这份结果。
     *
     * @param user 当前用户实体
     * @return 去重后的权限标识列表
     */
    public List<String> loadPermissions(SysUser user) {
        return loadPermissionSnapshot(user).getPermissions();
    }

    /**
     * 鍔犺浇鐢ㄦ埛鑿滃崟/椤甸潰鍏ュ彛鏉冮檺銆?
     *
     * <p>鐩綍銆佽彍鍗曡祫婧愪互鍙?`:page`/`:tree`/`:overview` 绫诲瀷鐨勯〉闈㈠叆鍙ｆ潈闄愪細鏀惧埌杩欓噷锛?
     * 鍓嶇鍙互鐢ㄥ畠鏉ュ垽鏂彍鍗曞拰璺敱鏄惁鍙銆?
     *
     * @param user 褰撳墠鐢ㄦ埛瀹炰綋
     * @return 鑿滃崟/椤甸潰鍏ュ彛鏉冮檺
     */
    public List<String> loadMenuPermissions(SysUser user) {
        return loadPermissionSnapshot(user).getMenuPermissions();
    }

    /**
     * 鍔犺浇鐢ㄦ埛鎸夐挳/鎿嶄綔鏉冮檺銆?
     *
     * @param user 褰撳墠鐢ㄦ埛瀹炰綋
     * @return 鎸夐挳/鎿嶄綔鏉冮檺
     */
    public List<String> loadButtonPermissions(SysUser user) {
        return loadPermissionSnapshot(user).getButtonPermissions();
    }

    /**
     * 计算用户登录后可访问的前端路由路径。
     *
     * <p>这里只会从“菜单资源”里取 `path`，因为按钮资源通常不对应路由。
     * 此外会额外补上个人中心页面，保证用户登录后至少能进入自己的资料页。
     *
     * @param user 当前用户实体
     * @return 去重后的路由路径列表
     */
    public List<String> loadPaths(SysUser user) {
        return loadPermissionSnapshot(user).getPaths();
    }

    /**
     * 涓诲姩娓呯悊鏌愪釜鐢ㄦ埛鐨勬潈闄愮紦瀛樸€?
     *
     * @param userId 鐢ㄦ埛 ID
     */
    public void evictUser(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(buildPermissionCacheKey(userId));
    }

    /**
     * 娓呯悊鎸囧畾瑙掕壊褰卞搷鍒扮殑鎵€鏈夌敤鎴锋潈闄愮紦瀛樸€?
     *
     * @param roleId 瑙掕壊 ID
     */
    public void evictUsersByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        sysUserRoleRepository.findAllByRoleId(roleId).stream()
                .map(SysUserRole::getUserId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        sysUserRepository.findAllByRoleId(roleId).stream()
                .map(SysUser::getId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        if (!userIds.isEmpty()) {
            stringRedisTemplate.delete(userIds.stream().map(this::buildPermissionCacheKey).toList());
        }
    }

    /**
     * 璧勬簮鏍戝彂鐢熷彉鍖栨椂娓呯悊鎵€鏈夌敤鎴锋潈闄愮紦瀛樸€?
     */
    public void evictAll() {
        Set<String> keys = stringRedisTemplate.keys(PERMISSION_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private PermissionSnapshot loadPermissionSnapshot(SysUser user) {
        String key = buildPermissionCacheKey(user.getId());
        String cachedValue = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cachedValue)) {
            try {
                return objectMapper.readValue(cachedValue, PermissionSnapshot.class);
            } catch (JacksonException exception) {
                log.warn("Permission cache deserialize failed, userId={}", user.getId(), exception);
                stringRedisTemplate.delete(key);
            }
        }

        PermissionSnapshot snapshot = buildPermissionSnapshot(user);
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(snapshot),
                    Duration.ofSeconds(Math.max(permissionCacheTtlSeconds, 1))
            );
        } catch (JacksonException exception) {
            log.warn("Permission cache serialize failed, userId={}", user.getId(), exception);
        }
        return snapshot;
    }

    private PermissionSnapshot buildPermissionSnapshot(SysUser user) {
        List<SysResource> resources = loadEffectiveResources(user);
        List<String> permissions = resources.stream()
                .map(this::resolvePermission)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> menuPermissions = resources.stream()
                .filter(this::isMenuPermissionResource)
                .map(this::resolvePermission)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> buttonPermissions = resources.stream()
                .filter(resource -> Objects.equals(resource.getResourceType(), BUTTON_TYPE))
                .filter(resource -> !isEntryPermission(resource))
                .map(this::resolvePermission)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        List<String> paths = buildPaths(resources);
        return new PermissionSnapshot(permissions, menuPermissions, buttonPermissions, paths);
    }

    private List<String> buildPaths(List<SysResource> resources) {
        return Stream.concat(
                        Stream.of(PROFILE_PATH),
                        resources.stream()
                                .filter(resource -> Objects.equals(resource.getResourceType(), MENU_TYPE))
                                .map(SysResource::getPath)
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private boolean isMenuPermissionResource(SysResource resource) {
        Integer resourceType = resource.getResourceType();
        return Objects.equals(resourceType, MENU_TYPE)
                || Objects.equals(resourceType, 1)
                || (Objects.equals(resourceType, BUTTON_TYPE) && isEntryPermission(resource));
    }

    private boolean isEntryPermission(SysResource resource) {
        String permission = resolvePermission(resource);
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        return ENTRY_PERMISSION_SUFFIXES.stream().anyMatch(permission::endsWith);
    }

    private String buildPermissionCacheKey(Long userId) {
        return PERMISSION_CACHE_PREFIX + userId;
    }

    /**
     * 加载用户绑定的角色 ID。
     *
     * <p>项目里同时兼容了两种角色存储方式：
     * 1. `sys_user_role` 关系表（多角色）；
     * 2. `sys_user.role_id` 字段（旧数据或单角色兜底）。
     *
     * @param user 当前用户实体
     * @return 去重后的角色 ID 列表
     */
    private List<Long> loadRoleIds(SysUser user) {
        List<Long> roleIds = sysUserRoleRepository.findAllByUserId(user.getId()).stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roleIds.isEmpty() && user.getRoleId() != null) {
            // 如果关系表没有数据，就退回到主表里的 roleId，兼容旧数据结构。
            return List.of(user.getRoleId());
        }
        return roleIds;
    }

    /**
     * 扩展并过滤资源 ID，得到真正可参与权限计算的资源集合。
     *
     * <p>这里会先补齐依赖资源，再过滤掉被禁用或父级已禁用的资源。
     * 这样最终返回给前端和安全框架的权限才会更稳定。
     *
     * @param resourceIds 角色直接分配到的资源 ID
     * @return 扩展并过滤后的有效资源 ID 列表
     */
    private List<Long> expandEffectiveResourceIds(List<Long> resourceIds) {
        List<SysResource> allResources = sysResourceRepository.findAllByOrderBySortNoAscIdAsc();
        Map<Long, SysResource> resourceMap = allResources.stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource));
        return resourcePermissionResolver.expandDependencies(resourceIds).stream()
                .distinct()
                .map(resourceMap::get)
                .filter(Objects::nonNull)
                // 不仅要看资源本身是否启用，还要看它的祖先节点是否都启用。
                .filter(resource -> isResourceEffectivelyEnabled(resource, resourceMap))
                .map(SysResource::getId)
                .toList();
    }

    /**
     * 计算用户最终生效的资源列表。
     *
     * <p>流程可以理解成：
     * 用户 -> 角色 -> 角色资源关系 -> 原始资源 -> 扩展依赖 -> 过滤禁用 -> 最终资源。
     *
     * @param user 当前用户实体
     * @return 生效资源列表
     */
    private List<SysResource> loadEffectiveResources(SysUser user) {
        List<Long> roleIds = loadEffectiveRoles(user).stream()
                .map(SysRole::getId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> assignedResourceIds = roleIds.stream()
                .flatMap(roleId -> sysRoleResourceRepository.findAllByRoleId(roleId).stream())
                .map(SysRoleResource::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (assignedResourceIds.isEmpty()) {
            return List.of();
        }

        List<Long> effectiveResourceIds = expandEffectiveResourceIds(assignedResourceIds);
        if (effectiveResourceIds.isEmpty()) {
            return List.of();
        }

        // 统一从完整资源表里按 ID 反查，保证返回顺序和 effectiveResourceIds 一致。
        Map<Long, SysResource> resourceMap = sysResourceRepository.findAllByOrderBySortNoAscIdAsc().stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource));
        return effectiveResourceIds.stream()
                .map(resourceMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 判断一个资源以及它的所有父级资源是否都处于启用状态。
     *
     * <p>比如按钮本身启用了，但它所在菜单被禁用了，那么这个按钮仍然不应该参与权限计算。
     *
     * @param resource 当前资源
     * @param resourceMap 资源 ID 到资源实体的映射
     * @return true 表示该资源链路整体可用
     */
    private boolean isResourceEffectivelyEnabled(SysResource resource, Map<Long, SysResource> resourceMap) {
        SysResource current = resource;
        while (current != null) {
            if (!isEnabled(current.getStatus())) {
                return false;
            }
            Long parentId = current.getParentId();
            if (parentId == null) {
                return true;
            }
            current = resourceMap.get(parentId);
            if (current == null) {
                return false;
            }
        }
        return false;
    }

    /**
     * 从资源上解析出最终权限标识。
     *
     * <p>优先使用 `permissionKey`，如果没有，再回退到 `permissionCode`。
     * 这样可以同时兼容新旧字段。
     *
     * @param resource 资源实体
     * @return 权限标识；如果两个字段都没有内容则返回 null
     */
    private String resolvePermission(SysResource resource) {
        if (StringUtils.hasText(resource.getPermissionKey())) {
            return resource.getPermissionKey().trim();
        }
        if (StringUtils.hasText(resource.getPermissionCode())) {
            return resource.getPermissionCode().trim();
        }
        return null;
    }

    /**
     * 判断状态值是否表示启用。
     *
     * @param status 状态值
     * @return true 表示启用
     */
    private boolean isEnabled(Integer status) {
        return Objects.equals(status, ENABLED_STATUS);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionSnapshot {
        private List<String> permissions = List.of();
        private List<String> menuPermissions = List.of();
        private List<String> buttonPermissions = List.of();
        private List<String> paths = List.of();
    }
}
