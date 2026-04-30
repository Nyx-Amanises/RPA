package com.rpa.manage.security;

import com.rpa.manage.domain.entity.SysResource;
import com.rpa.manage.domain.repository.SysResourceRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 资源依赖解析器。
 *
 * <p>角色分配资源时，前端勾选的未必是“完整权限集合”。
 * 例如只勾了某个按钮权限，真正做菜单展示和接口鉴权时，往往还需要把它所属菜单、
 * 菜单默认入口按钮等依赖资源一并补齐。
 *
 * <p>这个类就是专门做“补齐依赖”的，避免出现：
 * 1. 勾了按钮却找不到对应菜单；
 * 2. 只分配菜单却少了页面入口权限；
 * 3. 前端树回显和后端权限判断不一致。
 */
@Component
@RequiredArgsConstructor
public class ResourcePermissionResolver {

    /**
     * 约定 2 表示菜单资源。
     */
    private static final Integer MENU_TYPE = 2;
    /**
     * 约定 3 表示按钮资源。
     */
    private static final Integer BUTTON_TYPE = 3;
    /**
     * 这些后缀通常表示“页面入口类权限”，需要随着菜单一起补齐。
     */
    private static final Set<String> ENTRY_PERMISSION_SUFFIXES = Set.of(":page", ":tree", ":overview");

    private final SysResourceRepository sysResourceRepository;

    /**
     * 扩展一组资源 ID，把它们依赖的菜单或入口按钮一并补齐。
     *
     * @param resourceIds 原始资源 ID 集合
     * @return 按发现顺序去重后的资源 ID 列表
     */
    public List<Long> expandDependencies(Collection<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        List<SysResource> allResources = sysResourceRepository.findAllByOrderBySortNoAscIdAsc();
        Map<Long, SysResource> resourceMap = allResources.stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource));
        Map<Long, List<SysResource>> childrenMap = allResources.stream()
                .filter(resource -> resource.getParentId() != null)
                .collect(Collectors.groupingBy(SysResource::getParentId));

        LinkedHashSet<Long> expandedIds = new LinkedHashSet<>();
        resourceIds.stream()
                .filter(Objects::nonNull)
                // 每个资源都按“自己 -> 依赖资源”的方式展开，并用 LinkedHashSet 自动去重。
                .forEach(resourceId -> collectDependencies(resourceMap.get(resourceId), resourceMap, childrenMap, expandedIds));
        return expandedIds.stream().toList();
    }

    /**
     * 把某个资源本身及其依赖资源加入结果集。
     *
     * <p>规则分成两类：
     * 1. 如果当前是菜单，就补它下面的入口按钮；
     * 2. 如果当前是按钮，就向上找到所属菜单，并把菜单和入口按钮一起补上。
     *
     * @param resource 当前资源
     * @param resourceMap 资源映射
     * @param childrenMap 子资源映射
     * @param expandedIds 最终结果集
     */
    private void collectDependencies(
            SysResource resource,
            Map<Long, SysResource> resourceMap,
            Map<Long, List<SysResource>> childrenMap,
            LinkedHashSet<Long> expandedIds
    ) {
        if (resource == null) {
            return;
        }
        expandedIds.add(resource.getId());
        if (Objects.equals(resource.getResourceType(), MENU_TYPE)) {
            // 直接勾选菜单时，顺手把它默认需要的入口权限补上。
            appendEntryButtons(resource, childrenMap, expandedIds);
            return;
        }
        if (!Objects.equals(resource.getResourceType(), BUTTON_TYPE)) {
            return;
        }

        SysResource menu = findNearestMenu(resource, resourceMap);
        if (menu != null) {
            // 勾选按钮时，前端菜单也需要能显示出来，所以菜单本身也要补上。
            expandedIds.add(menu.getId());
            appendEntryButtons(menu, childrenMap, expandedIds);
        }
    }

    /**
     * 从当前资源开始，向上找到最近的菜单节点。
     *
     * @param resource 当前资源
     * @param resourceMap 资源映射
     * @return 最近的菜单资源；找不到则返回 null
     */
    private SysResource findNearestMenu(SysResource resource, Map<Long, SysResource> resourceMap) {
        SysResource current = resource;
        while (current != null) {
            if (Objects.equals(current.getResourceType(), MENU_TYPE)) {
                return current;
            }
            Long parentId = current.getParentId();
            current = parentId == null ? null : resourceMap.get(parentId);
        }
        return null;
    }

    /**
     * 为指定菜单补齐入口按钮。
     *
     * <p>这里只会补“按钮类型 + 权限后缀匹配入口规则”的资源，
     * 不会把菜单下的所有按钮都无脑加进来。
     *
     * @param menu 菜单资源
     * @param childrenMap 子资源映射
     * @param expandedIds 最终结果集
     */
    private void appendEntryButtons(
            SysResource menu,
            Map<Long, List<SysResource>> childrenMap,
            LinkedHashSet<Long> expandedIds
    ) {
        childrenMap.getOrDefault(menu.getId(), List.of()).stream()
                .filter(resource -> Objects.equals(resource.getResourceType(), BUTTON_TYPE))
                .filter(this::isEntryPermission)
                .map(SysResource::getId)
                .forEach(expandedIds::add);
    }

    /**
     * 判断某个按钮资源是不是“入口类权限”。
     *
     * @param resource 资源实体
     * @return true 表示应当随菜单一起补齐
     */
    private boolean isEntryPermission(SysResource resource) {
        String permission = resolvePermission(resource);
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        return ENTRY_PERMISSION_SUFFIXES.stream().anyMatch(permission::endsWith);
    }

    /**
     * 从资源里解析出可用于判断的权限标识。
     *
     * @param resource 资源实体
     * @return 优先取 `permissionKey`，否则取 `permissionCode`
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
}
