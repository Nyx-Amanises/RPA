package com.rpa.manage.service.impl.system;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.common.util.SecurityUtils;
import com.rpa.manage.domain.dto.system.ChangePasswordRequest;
import com.rpa.manage.domain.dto.system.ProfileUpdateRequest;
import com.rpa.manage.domain.dto.system.ResourceUpsertRequest;
import com.rpa.manage.domain.dto.system.RolePageQuery;
import com.rpa.manage.domain.dto.system.RoleResourceAssignRequest;
import com.rpa.manage.domain.dto.system.RoleUpsertRequest;
import com.rpa.manage.domain.dto.system.UserPageQuery;
import com.rpa.manage.domain.dto.system.UserStatusRequest;
import com.rpa.manage.domain.dto.system.UserUpsertRequest;
import com.rpa.manage.domain.entity.SysResource;
import com.rpa.manage.domain.entity.SysRole;
import com.rpa.manage.domain.entity.SysRoleResource;
import com.rpa.manage.domain.entity.SysUser;
import com.rpa.manage.domain.entity.SysUserRole;
import com.rpa.manage.domain.repository.SysResourceRepository;
import com.rpa.manage.domain.repository.SysRoleRepository;
import com.rpa.manage.domain.repository.SysRoleResourceRepository;
import com.rpa.manage.domain.repository.SysUserRepository;
import com.rpa.manage.domain.repository.SysUserRoleRepository;
import com.rpa.manage.security.PermissionService;
import com.rpa.manage.security.ResourcePermissionResolver;
import com.rpa.manage.service.system.SystemService;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 系统管理服务实现类。
 *
 * <p>这个类集中处理四大类业务：
 * 1. 当前登录用户自己的资料维护；
 * 2. 管理员对用户的增删改查；
 * 3. 角色与资源（权限）的维护；
 * 4. 资源树、角色资源关系等权限基础数据的组装。
 *
 * <p>如果你是第一次阅读这份代码，建议先看公开方法，再看下面的辅助方法，
 * 这样会更容易理解“请求进来以后，数据是怎样一步步流转到数据库里的”。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    /**
     * 新建用户、重置密码时使用的默认密码。
     */
    private static final String DEFAULT_PASSWORD = "123456";
    /**
     * 约定 1 表示“启用”。这里统一抽成常量，避免代码里到处直接写魔法值。
     */
    private static final Integer ENABLED_STATUS = 1;

    /*
     * 下面这些 Repository 负责访问数据库。
     * 你可以把它们理解成“表操作入口”，这个类自己不直接写 SQL，而是通过它们完成查询和保存。
     */
    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository  sysRoleRepository;
    private final SysResourceRepository sysResourceRepository;
    private final SysRoleResourceRepository sysRoleResourceRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResourcePermissionResolver resourcePermissionResolver;
    private final PermissionService permissionService;

    /**
     * 头像文件在磁盘中的存放目录；如果配置文件没有指定，就默认使用 uploads/avatars。
     */
    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;


    /*
     * ==================== 个人中心 ====================
     * 这一部分只处理“当前登录用户自己”的资料，不涉及管理员去操作别人账号。
     */

    /**
     * 更新当前登录用户的个人资料。
     *
     * <p>这里只允许修改姓名、邮箱、手机号等基础字段，
     * 不会在这里处理角色、密码等更敏感的数据。
     *
     * @param request 前端提交的个人资料
     * @return 更新后的个人资料结果
     */
    @Override
    @Transactional
    public Map<String, Object> updateProfile(ProfileUpdateRequest request) {
        // 先拿到数据库中的当前用户，再做唯一性校验，避免把别人的邮箱或手机号改重。
        SysUser user = getCurrentDbUser();
        log.info("更新个人信息，userId={}, username={}", user.getId(), user.getUsername());
        validateUserUniqueFields(request.getEmail(), request.getMobile(), user.getId(), user.getUsername());
        user.setRealName(request.getRealName().trim());
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setMobile(request.getMobile().trim());
        sysUserRepository.save(user);
        log.info("个人信息更新完成，userId={}, username={}", user.getId(), user.getUsername());
        return toUserProfile(user, loadRolesByUserId(user.getId()));
    }

    /**
     * 扩展角色资源 ID。
     *
     * <p>前端勾选的可能只是菜单，也可能只是按钮。
     * 为了让权限树回显和接口鉴权更稳定，这里会把依赖资源一起补齐。
     *
     * @param resourceIds 原始资源 ID 集合
     * @return 补齐依赖后的资源 ID 列表
     */
    private List<Long> expandRoleResourceIds(Collection<Long> resourceIds) {
        return resourcePermissionResolver.expandDependencies(resourceIds);
    }

    /**
     * 上传当前登录用户头像。
     *
     * <p>处理流程：校验文件 -> 创建目录 -> 保存文件 -> 回写头像 URL。
     *
     * @param file 前端上传的头像文件
     * @return 包含头像地址和文件名的结果
     */
    @Override
    @Transactional
    public Map<String, Object> uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            throw new BusinessException("头像仅支持 jpg、jpeg、png、gif、webp 格式");
        }

        SysUser user = getCurrentDbUser();
        log.info("上传头像，userId={}, username={}, originalFilename={}",
                user.getId(), user.getUsername(), originalFilename);
        // 先把配置目录转换成绝对路径，后续创建目录和保存文件都会更稳妥。
        Path uploadDirectory = Paths.get(avatarDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDirectory);
            // 文件名里带上用户 ID 和时间戳，尽量避免同名覆盖。
            String fileName = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + "." + extension;
            Path targetPath = uploadDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/avatars/")
                    .path(fileName)
                    .toUriString();
            user.setAvatarUrl(avatarUrl);
            sysUserRepository.save(user);
            log.info("头像上传完成，userId={}, username={}, avatarUrl={}",
                    user.getId(), user.getUsername(), avatarUrl);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("avatarUrl", avatarUrl);
            result.put("fileName", fileName);
            return result;
        } catch (IOException exception) {
            throw new BusinessException("头像上传失败，请稍后重试");
        }
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param request 包含原密码、新密码、确认密码的请求对象
     * @return 空结果，表示修改成功
     */
    @Override
    @Transactional
    public Map<String, Object> changePassword(ChangePasswordRequest request) {
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        SysUser user = getCurrentDbUser();
        log.info("修改密码，userId={}, username={}", user.getId(), user.getUsername());
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        user.setPasswordHash(encodedPassword);
        sysUserRepository.save(user);
        log.info("密码修改完成，userId={}, username={}", user.getId(), user.getUsername());
        return Map.of();
    }


    /*
     * ==================== 用户管理 ====================
     * 这一部分是管理员管理系统用户的核心逻辑。
     */

    /**
     * 分页查询用户列表。
     *
     * <p>这类查询除了用户主表数据，还要补充用户拥有的角色信息，
     * 因此前半段负责查库，后半段负责组装给前端展示的数据结构。
     *
     * @param query 查询条件与分页参数
     * @return 用户分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> usersPage(UserPageQuery query) {
        /*
         * 分页查询分两步：
         * 1. 先用 Specification 动态拼接 where 条件；
         * 2. 再按页查出用户，并批量补充角色信息。
         */
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );
        Specification<SysUser> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getUsername())) {
                predicates.add(cb.like(root.get("username"), "%" + query.getUsername().trim() + "%"));
            }
            if (StringUtils.hasText(query.getRealName())) {
                predicates.add(cb.like(root.get("realName"), "%" + query.getRealName().trim() + "%"));
            }
            if (query.getRoleId() != null) {
                // 角色筛选要兼容两种存储方式：sys_user.role_id 和 sys_user_role 关系表。
                Set<Long> userIds = new LinkedHashSet<>();
                sysUserRoleRepository.findAllByRoleId(query.getRoleId())
                        .forEach(item -> userIds.add(item.getUserId()));
                if (userIds.isEmpty()) {
                    predicates.add(cb.equal(root.get("roleId"), query.getRoleId()));
                } else {
                    Predicate roleIdPredicate = root.get("id").in(userIds);
                    Predicate fallbackPredicate = cb.equal(root.get("roleId"), query.getRoleId());
                    predicates.add(cb.or(roleIdPredicate, fallbackPredicate));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<SysUser> page = sysUserRepository.findAll(specification, pageable);
        Map<Long, List<SysRole>> rolesByUserId = loadRolesByUserIds(page.getContent().stream().map(SysUser::getId).toList());
        List<Map<String, Object>> list = page.getContent().stream()
                .map(user -> toUserPageItem(user, rolesByUserId.getOrDefault(user.getId(), List.of())))
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 创建系统用户。
     *
     * <p>这个项目会同时维护 sys_user.role_id 和 sys_user_role 两份角色数据，
     * 所以创建用户后，还要同步写入用户角色关系表。
     *
     * @param request 用户新增请求
     * @return 新创建用户的展示结果
     */
    @Override
    @Transactional
    public Map<String, Object> createUser(UserUpsertRequest request) {
        validateRoleIds(request.getRoleIds(), Set.of());
        if (sysUserRepository.existsByUsername(request.getUsername().trim())) {
            throw new BusinessException("用户名已存在");
        }
        if (sysUserRepository.existsByMobile(request.getMobile().trim())) {
            throw new BusinessException("手机号已存在");
        }
        String email = normalizeNullable(request.getEmail());
        if (email != null && sysUserRepository.existsByEmail(email)) {
            throw new BusinessException("邮箱已存在");
        }
        log.info("创建用户，username={}, roleIds={}", request.getUsername(), request.getRoleIds());

        /*
         * 这里先写用户主表，再写用户-角色关系表。
         * 主表中的 roleId 默认取第一个角色，表示“主角色”。
         */
        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setRealName(request.getRealName().trim());
        user.setEmail(email);
        user.setMobile(request.getMobile().trim());
        user.setStatus(request.getStatus());
        user.setRoleId(request.getRoleIds().get(0));
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        user.setPassword(encodedPassword);
        user.setPasswordHash(encodedPassword);
        sysUserRepository.save(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        permissionService.evictUser(user.getId());
        log.info("创建用户完成，userId={}, username={}, roleIds={}",
                user.getId(), user.getUsername(), request.getRoleIds());
        return toUserPageItem(user, loadRolesByUserId(user.getId()));
    }

    /**
     * 修改指定用户信息。
     *
     * @param id 目标用户 ID
     * @param request 用户修改请求
     * @return 修改后的用户信息
     */
    @Override
    @Transactional
    public Map<String, Object> updateUser(Long id, UserUpsertRequest request) {
        log.info("修改用户，targetUserId={}, username={}, roleIds={}", id, request.getUsername(), request.getRoleIds());
        SysUser user = sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        validateRoleIds(request.getRoleIds(), loadAssignedRoleIds(user));
        if (sysUserRepository.existsByUsernameAndIdNot(request.getUsername().trim(), id)) {
            throw new BusinessException("用户名已存在");
        }
        if (sysUserRepository.existsByMobileAndIdNot(request.getMobile().trim(), id)) {
            throw new BusinessException("手机号已存在");
        }
        String email = normalizeNullable(request.getEmail());
        if (email != null && sysUserRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException("邮箱已存在");
        }
        user.setUsername(request.getUsername().trim());
        user.setRealName(request.getRealName().trim());
        user.setEmail(email);
        user.setMobile(request.getMobile().trim());
        user.setStatus(request.getStatus());
        user.setRoleId(request.getRoleIds().get(0));
        sysUserRepository.save(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        permissionService.evictUser(user.getId());
        log.info("修改用户完成，targetUserId={}, username={}, roleIds={}",
                user.getId(), user.getUsername(), request.getRoleIds());
        return toUserPageItem(user, loadRolesByUserId(user.getId()));
    }

    /**
     * 将指定用户密码重置为系统默认密码。
     *
     * @param id 目标用户 ID
     * @return 包含用户 ID 和默认密码的结果
     */
    @Override
    @Transactional
    public Map<String, Object> resetPassword(Long id) {
        log.info("重置用户密码，targetUserId={}", id);
        SysUser user = sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        user.setPassword(encodedPassword);
        user.setPasswordHash(encodedPassword);
        sysUserRepository.save(user);
        log.info("重置用户密码完成，targetUserId={}, username={}", user.getId(), user.getUsername());
        return Map.of("id", user.getId(), "defaultPassword", DEFAULT_PASSWORD);
    }

    /**
     * 修改指定用户状态。
     *
     * @param id 目标用户 ID
     * @param request 状态请求对象
     * @return 包含最新状态的结果
     */
    @Override
    @Transactional
    public Map<String, Object> changeUserStatus(Long id, UserStatusRequest request) {
        log.info("修改用户状态，targetUserId={}, newStatus={}", id, request.getStatus());
        SysUser user = sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (Objects.equals(SecurityUtils.currentUserId(), id) && !Objects.equals(request.getStatus(), 1)) {
            throw new BusinessException("不能禁用当前登录用户");
        }
        user.setStatus(request.getStatus());
        sysUserRepository.save(user);
        permissionService.evictUser(user.getId());
        log.info("修改用户状态完成，targetUserId={}, username={}, status={}",
                user.getId(), user.getUsername(), user.getStatus());
        return Map.of("id", user.getId(), "status", user.getStatus());
    }

    /**
     * 删除指定用户。
     *
     * @param id 目标用户 ID
     * @return 删除结果
     */
    @Override
    @Transactional
    public Map<String, Object> deleteUser(Long id) {
        log.info("删除用户，targetUserId={}", id);
        SysUser user = sysUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (Objects.equals(SecurityUtils.currentUserId(), id)) {
            throw new BusinessException("不能删除当前登录用户");
        }
        sysUserRoleRepository.deleteByUserId(id);
        sysUserRepository.delete(user);
        permissionService.evictUser(id);
        log.info("删除用户完成，targetUserId={}, username={}", id, user.getUsername());
        return Map.of("id", id);
    }


    /*
     * ==================== 角色管理 ====================
     * 角色决定“某一类用户可以访问哪些资源、执行哪些操作”。
     */

    /**
     * 分页查询角色列表。
     *
     * <p>除了角色本身信息，这里还会顺手补充用户数量和权限摘要，
     * 让前端无需再次发请求就能直接展示完整列表。
     *
     * @param query 查询条件与分页参数
     * @return 角色分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> rolesPage(RolePageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );
        Specification<SysRole> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getRoleCode())) {
                predicates.add(cb.like(root.get("roleCode"), "%" + query.getRoleCode().trim() + "%"));
            }
            if (StringUtils.hasText(query.getRoleName())) {
                predicates.add(cb.like(root.get("roleName"), "%" + query.getRoleName().trim() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<SysRole> page = sysRoleRepository.findAll(specification, pageable);
        // 把用户数和权限摘要提前算好，避免在 stream 中重复查询数据库。
        Map<Long, Long> roleUserCountMap = new HashMap<>();
        Map<Long, String> permissionSummaryMap = new HashMap<>();
        for (SysRole role : page.getContent()) {
            long userCount = countUsersByRoleId(role.getId());
            roleUserCountMap.put(role.getId(), userCount);
            permissionSummaryMap.put(role.getId(), buildRolePermissionSummary(role.getId()));
        }
        List<Map<String, Object>> list = page.getContent().stream()
                .map(role -> toRolePageItem(role, roleUserCountMap.getOrDefault(role.getId(), 0L), permissionSummaryMap.get(role.getId())))
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 创建角色。
     *
     * @param request 角色新增请求
     * @return 新角色的展示结果
     */
    @Override
    @Transactional
    public Map<String, Object> createRole(RoleUpsertRequest request) {
        log.info("创建角色，roleCode={}, roleName={}", request.getRoleCode(), request.getRoleName());
        if (sysRoleRepository.existsByRoleCode(request.getRoleCode().trim())) {
            throw new BusinessException("角色编码已存在");
        }
        if (sysRoleRepository.existsByRoleName(request.getRoleName().trim())) {
            throw new BusinessException("角色名称已存在");
        }
        SysRole role = new SysRole();
        applyRole(role, request);
        sysRoleRepository.save(role);
        permissionService.evictUsersByRoleId(role.getId());
        log.info("创建角色完成，roleId={}, roleCode={}, roleName={}", role.getId(), role.getRoleCode(), role.getRoleName());
        return toRolePageItem(role, 0L, "无权限");
    }

    /**
     * 修改角色。
     *
     * <p>角色编码创建后会被当作稳定标识使用，因此这里明确禁止修改。
     *
     * @param id 角色 ID
     * @param request 角色修改请求
     * @return 修改后的角色信息
     */
    @Override
    @Transactional
    public Map<String, Object> updateRole(Long id, RoleUpsertRequest request) {
        log.info("修改角色，roleId={}, roleCode={}, roleName={}", id, request.getRoleCode(), request.getRoleName());
        SysRole role = sysRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        if (!Objects.equals(role.getRoleCode(), request.getRoleCode().trim())) {
            throw new BusinessException("角色编码创建后不允许修改");
        }
        if (sysRoleRepository.existsByRoleNameAndIdNot(request.getRoleName().trim(), id)) {
            throw new BusinessException("角色名称已存在");
        }
        applyRole(role, request);
        sysRoleRepository.save(role);
        permissionService.evictUsersByRoleId(role.getId());
        log.info("修改角色完成，roleId={}, roleCode={}, roleName={}", role.getId(), role.getRoleCode(), role.getRoleName());
        return toRolePageItem(role, countUsersByRoleId(id), buildRolePermissionSummary(id));
    }

    /**
     * 查询角色当前已分配的资源。
     *
     * <p>这里返回的是“扩展后的资源 ID”，这样前端权限树回显时，
     * 菜单与按钮之间的依赖关系也能一起显示出来。
     *
     * @param id 角色 ID
     * @return 角色拥有的资源 ID 列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<Long> roleResources(Long id) {
        sysRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        List<Long> assignedResourceIds = sysRoleResourceRepository.findAllByRoleId(id).stream()
                .map(SysRoleResource::getResourceId)
                .distinct()
                .toList();
        if (assignedResourceIds.isEmpty()) {
            return List.of();
        }
        return expandRoleResourceIds(assignedResourceIds);
    }

    /**
     * 给角色分配资源。
     *
     * <p>这里采用“覆盖式分配”：先删掉旧关系，再整批写入新关系，
     * 保证数据库中的最终状态和前端勾选结果完全一致。
     *
     * @param id 角色 ID
     * @param request 资源分配请求
     * @return 更新后的角色信息
     */
    @Override
    @Transactional
    public Map<String, Object> assignRoleResources(Long id, RoleResourceAssignRequest request) {
        log.info("分配角色资源，roleId={}, resourceIds={}", id, request.getResourceIds());
        SysRole role = sysRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        validateResourceIds(request.getResourceIds());
        // 先补齐菜单/按钮之间的依赖，再覆盖原有的角色资源关系。
        List<Long> expandedResourceIds = expandRoleResourceIds(request.getResourceIds());
        sysRoleResourceRepository.deleteByRoleId(id);
        sysRoleResourceRepository.flush();
        List<SysRoleResource> mappings = expandedResourceIds.stream()
                .map(resourceId -> {
                    SysRoleResource relation = new SysRoleResource();
                    relation.setRoleId(id);
                    relation.setResourceId(resourceId);
                    relation.setGrantTime(LocalDateTime.now());
                    return relation;
                })
                .toList();
        sysRoleResourceRepository.saveAll(mappings);
        permissionService.evictUsersByRoleId(id);
        log.info("分配角色资源完成，roleId={}, resourceCount={}", id, mappings.size());
        return toRolePageItem(role, countUsersByRoleId(id), buildRolePermissionSummary(id));
    }

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     * @return 删除结果
     */
    @Override
    @Transactional
    public Map<String, Object> deleteRole(Long id) {
        log.info("删除角色，roleId={}", id);
        SysRole role = sysRoleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        if (countUsersByRoleId(id) > 0) {
            throw new BusinessException("该角色下仍存在用户，不能删除");
        }
        sysRoleResourceRepository.deleteByRoleId(id);
        sysRoleRepository.delete(role);
        permissionService.evictUsersByRoleId(id);
        log.info("删除角色完成，roleId={}, roleCode={}, roleName={}", id, role.getRoleCode(), role.getRoleName());
        return Map.of("id", id);
    }


    /*
     * ==================== 资源管理 ====================
     * 资源既可以表示页面菜单，也可以表示按钮权限，是权限系统的基础数据。
     */

    /**
     * 查询资源树。
     *
     * @param resourceType 可选的资源类型过滤条件
     * @param resourceName 可选的资源名称模糊查询条件
     * @return 组装好的树形结构
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> resourceTree(Integer resourceType, String resourceName) {
        List<SysResource> resources = sysResourceRepository.findAllByOrderBySortNoAscIdAsc();
        List<SysResource> filtered = filterResources(resources, resourceType, resourceName);
        return buildResourceTree(filtered, null);
    }

    /**
     * 创建资源。
     *
     * @param request 资源新增请求
     * @return 新资源的展示结果
     */
    @Override
    @Transactional
    public Map<String, Object> createResource(ResourceUpsertRequest request) {
        log.info("创建资源，resourceCode={}, resourceName={}, resourceType={}",
                request.getResourceCode(), request.getResourceName(), request.getResourceType());
        validateResourceRequest(null, request);
        SysResource resource = new SysResource();
        applyResource(resource, request);
        sysResourceRepository.save(resource);
        permissionService.evictAll();
        log.info("创建资源完成，resourceId={}, resourceCode={}, resourceName={}",
                resource.getId(), resource.getResourceCode(), resource.getResourceName());
        return toResourceItem(resource, List.of());
    }

    /**
     * 修改资源。
     *
     * @param id 资源 ID
     * @param request 资源修改请求
     * @return 修改后的资源信息
     */
    @Override
    @Transactional
    public Map<String, Object> updateResource(Long id, ResourceUpsertRequest request) {
        log.info("修改资源，resourceId={}, resourceCode={}, resourceName={}",
                id, request.getResourceCode(), request.getResourceName());
        SysResource resource = sysResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资源不存在"));
        validateResourceRequest(id, request);
        applyResource(resource, request);
        sysResourceRepository.save(resource);
        permissionService.evictAll();
        log.info("修改资源完成，resourceId={}, resourceCode={}, resourceName={}",
                resource.getId(), resource.getResourceCode(), resource.getResourceName());
        return toResourceItem(resource, List.of());
    }

    /**
     * 删除资源。
     *
     * <p>删除前要确保它没有子资源，同时也没有被任何角色引用。
     *
     * @param id 资源 ID
     * @return 删除结果
     */
    @Override
    @Transactional
    public Map<String, Object> deleteResource(Long id) {
        log.info("删除资源，resourceId={}", id);
        SysResource resource = sysResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资源不存在"));
        if (sysResourceRepository.existsByParentId(id)) {
            throw new BusinessException("请先删除下级资源");
        }
        if (sysRoleResourceRepository.existsByResourceId(id)) {
            throw new BusinessException("该资源已分配给角色，不能删除");
        }
        sysResourceRepository.delete(resource);
        permissionService.evictAll();
        log.info("删除资源完成，resourceId={}, resourceCode={}, resourceName={}",
                id, resource.getResourceCode(), resource.getResourceName());
        return Map.of("id", id);
    }


    /*
     * ==================== 内部辅助方法 ====================
     * 下面的方法不直接对外暴露，主要负责校验、组装、统计和结果转换。
     */

    /**
     * 把角色请求对象中的字段拷贝到实体对象中。
     *
     * @param role 目标角色实体
     * @param request 前端提交的角色数据
     */
    private void applyRole(SysRole role, RoleUpsertRequest request) {
        if (role.getId() == null) {
            role.setRoleCode(request.getRoleCode().trim());
        }
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(normalizeNullable(request.getDescription()));
        role.setRemark(normalizeNullable(request.getRemark()));
        role.setStatus(request.getStatus());
    }

    /**
     * 把资源请求对象中的字段拷贝到资源实体。
     *
     * <p>这里还会同步设置 permissionKey 和 permissionCode，
     * 让新旧两套字段保持一致。
     *
     * @param resource 目标资源实体
     * @param request 前端提交的资源数据
     */
    private void applyResource(SysResource resource, ResourceUpsertRequest request) {
        resource.setParentId(request.getParentId());
        resource.setResourceName(request.getResourceName().trim());
        resource.setResourceCode(request.getResourceCode().trim());
        resource.setResourceType(request.getResourceType());
        resource.setPath(normalizeNullable(request.getPath()));
        resource.setComponent(normalizeNullable(request.getComponent()));
        // 这里让 permissionKey 和 permissionCode 保持同值，方便兼容旧字段。
        String permissionKey = normalizeNullable(request.getPermissionKey());
        resource.setPermissionKey(permissionKey);
        resource.setPermissionCode(permissionKey);
        resource.setIcon(normalizeNullable(request.getIcon()));
        resource.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        resource.setRemark(normalizeNullable(request.getRemark()));
        resource.setStatus(request.getStatus());
    }

    /**
     * 校验资源新增/修改请求是否合法。
     *
     * <p>主要校验三类问题：上级资源是否合法、资源编码是否重复、权限标识是否重复。
     *
     * @param id 资源 ID；新增时为 null
     * @param request 前端提交的资源请求
     */
    private void validateResourceRequest(Long id, ResourceUpsertRequest request) {
        Long parentId = request.getParentId();
        if (parentId != null) {
            if (Objects.equals(parentId, id)) {
                throw new BusinessException("上级资源不能选择自己");
            }
            if (!sysResourceRepository.existsById(parentId)) {
                throw new BusinessException("上级资源不存在");
            }
        }
        String resourceCode = request.getResourceCode().trim();
        if (id == null) {
            if (sysResourceRepository.existsByResourceCode(resourceCode)) {
                throw new BusinessException("资源编码已存在");
            }
        } else if (sysResourceRepository.existsByResourceCodeAndIdNot(resourceCode, id)) {
            throw new BusinessException("资源编码已存在");
        }

        String permissionKey = normalizeNullable(request.getPermissionKey());
        if (permissionKey != null) {
            if (id == null) {
                if (sysResourceRepository.existsByPermissionKey(permissionKey)
                        || sysResourceRepository.existsByPermissionCode(permissionKey)) {
                    throw new BusinessException("权限标识已存在");
                }
            } else if (sysResourceRepository.existsByPermissionKeyAndIdNot(permissionKey, id)
                    || sysResourceRepository.existsByPermissionCodeAndIdNot(permissionKey, id)) {
                throw new BusinessException("权限标识已存在");
            }
        }
    }

    /**
     * 判断状态值是否为启用。
     *
     * @param status 状态值
     * @return true 表示启用，false 表示禁用或空值
     */
    private boolean isEnabled(Integer status) {
        return Objects.equals(status, ENABLED_STATUS);
    }

    /**
     * 校验角色 ID 集合是否合法。
     *
     * @param roleIds 待校验的角色 ID 集合
     * @param allowedDisabledRoleIds 允许保留的禁用角色 ID
     */
    private void validateRoleIds(Collection<Long> roleIds, Set<Long> allowedDisabledRoleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException("角色不能为空");
        }
        List<Long> distinctRoleIds = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctRoleIds.size() != roleIds.size()) {
            throw new BusinessException("角色数据不合法");
        }
        List<SysRole> roles = sysRoleRepository.findAllById(distinctRoleIds);
        if (roles.size() != distinctRoleIds.size()) {
            throw new BusinessException("存在无效角色");
        }
        List<String> forbiddenRoleNames = roles.stream()
                .filter(role -> !isEnabled(role.getStatus()))
                .filter(role -> !allowedDisabledRoleIds.contains(role.getId()))
                .map(SysRole::getRoleName)
                .toList();
        if (!forbiddenRoleNames.isEmpty()) {
            throw new BusinessException("禁用角色不允许新绑定：" + String.join("、", forbiddenRoleNames));
        }
    }

    /**
     * 校验资源 ID 集合是否合法。
     *
     * @param resourceIds 待校验的资源 ID 集合
     */
    private void validateResourceIds(Collection<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            throw new BusinessException("资源不能为空");
        }
        List<Long> distinctIds = resourceIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.size() != resourceIds.size()) {
            throw new BusinessException("资源数据不合法");
        }
        List<SysResource> resources = sysResourceRepository.findAllById(distinctIds);
        if (resources.size() != distinctIds.size()) {
            throw new BusinessException("存在无效资源");
        }
    }

    /**
     * 覆盖式替换用户角色关系。
     *
     * <p>做法和角色资源分配类似：先删旧关系，再写新关系。
     *
     * @param userId 用户 ID
     * @param roleIds 新的角色 ID 列表
     */
    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        // 先清空旧关系，避免留下历史脏数据。
        sysUserRoleRepository.deleteByUserId(userId);
        sysUserRoleRepository.flush();
        List<SysUserRole> relations = roleIds.stream()
                .distinct()
                .map(roleId -> {
                    SysUserRole relation = new SysUserRole();
                    relation.setUserId(userId);
                    relation.setRoleId(roleId);
                    return relation;
                })
                .toList();
        sysUserRoleRepository.saveAll(relations);
    }

    /**
     * 校验用户唯一字段。
     *
     * @param email 邮箱
     * @param mobile 手机号
     * @param id 当前用户 ID，用于更新时排除自己
     * @param username 用户名
     */
    private void validateUserUniqueFields(String email, String mobile, Long id, String username) {
        if (sysUserRepository.existsByMobileAndIdNot(mobile.trim(), id)) {
            throw new BusinessException("手机号已存在");
        }
        if (email != null && sysUserRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException("邮箱已存在");
        }
        if (sysUserRepository.existsByUsernameAndIdNot(username.trim(), id)) {
            throw new BusinessException("用户名已存在");
        }
    }

    /**
     * 从数据库重新查询当前登录用户。
     *
     * <p>真正修改数据前，最好从数据库拿最新记录，而不是只依赖安全上下文中的缓存信息。
     *
     * @return 当前登录用户实体
     */
    private SysUser getCurrentDbUser() {
        return sysUserRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException("当前登录用户不存在"));
    }

    /**
     * 读取某个用户当前已经绑定的角色 ID。
     *
     * @param user 用户实体
     * @return 已绑定角色 ID 集合
     */
    private Set<Long> loadAssignedRoleIds(SysUser user) {
        Set<Long> roleIds = new LinkedHashSet<>();
        if (user.getRoleId() != null) {
            roleIds.add(user.getRoleId());
        }
        sysUserRoleRepository.findAllByUserId(user.getId()).stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .forEach(roleIds::add);
        return roleIds;
    }

    /**
     * 批量加载用户对应的角色列表。
     *
     * <p>这里会合并 sys_user.role_id 与 sys_user_role 两处数据，
     * 以兼容项目里同时存在的“单角色字段”和“多角色关系表”。
     *
     * @param userIds 用户 ID 列表
     * @return key 为用户 ID，value 为角色列表的映射
     */
    private Map<Long, List<SysRole>> loadRolesByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        /*
         * 先拿到关系表里的角色，再补充 sys_user.role_id 里的主角色。
         * 这样无论旧数据还是新数据，都能统一组装出来。
         */
        List<SysUserRole> relations = sysUserRoleRepository.findAll().stream()
                .filter(item -> userIds.contains(item.getUserId()))
                .toList();
        Set<Long> roleIds = relations.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SysUser> users = sysUserRepository.findAllById(userIds);
        users.stream()
                .map(SysUser::getRoleId)
                .filter(Objects::nonNull)
                .forEach(roleIds::add);

        Map<Long, SysRole> roleMap = sysRoleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        Map<Long, LinkedHashSet<Long>> userRoleIdsMap = new HashMap<>();
        for (SysUser user : users) {
            userRoleIdsMap.computeIfAbsent(user.getId(), key -> new LinkedHashSet<>());
            if (user.getRoleId() != null) {
                userRoleIdsMap.get(user.getId()).add(user.getRoleId());
            }
        }
        for (SysUserRole relation : relations) {
            userRoleIdsMap.computeIfAbsent(relation.getUserId(), key -> new LinkedHashSet<>())
                    .add(relation.getRoleId());
        }

        Map<Long, List<SysRole>> result = new HashMap<>();
        for (Map.Entry<Long, LinkedHashSet<Long>> entry : userRoleIdsMap.entrySet()) {
            List<SysRole> roles = entry.getValue().stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            result.put(entry.getKey(), roles);
        }
        return result;
    }

    /**
     * 按单个用户 ID 查询角色列表。
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    private List<SysRole> loadRolesByUserId(Long userId) {
        return loadRolesByUserIds(List.of(userId)).getOrDefault(userId, List.of());
    }

    /**
     * 统计某个角色下有多少用户。
     *
     * @param roleId 角色 ID
     * @return 用户数量
     */
    private long countUsersByRoleId(Long roleId) {
        Set<Long> userIds = new LinkedHashSet<>();
        sysUserRoleRepository.findAllByRoleId(roleId)
                .forEach(item -> userIds.add(item.getUserId()));
        sysUserRepository.findAllByRoleId(roleId)
                .forEach(user -> userIds.add(user.getId()));
        return userIds.size();
    }

    /**
     * 构造角色权限摘要。
     *
     * <p>这个摘要主要给角色分页列表使用，让前端能直接看到角色大概拥有哪些权限。
     *
     * @param roleId 角色 ID
     * @return 用顿号拼接后的权限名称摘要
     */
    private String buildRolePermissionSummary(Long roleId) {
        List<Long> resourceIds = sysRoleResourceRepository.findAllByRoleId(roleId).stream()
                .map(SysRoleResource::getResourceId)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return "无权限";
        }
        // 摘要也要基于扩展后的资源来算，否则只看按钮时很难看出它属于哪个菜单。
        List<Long> effectiveResourceIds = expandRoleResourceIds(resourceIds);
        List<String> names = sysResourceRepository.findAllById(effectiveResourceIds).stream()
                .sorted(Comparator.comparing(resource -> effectiveResourceIds.indexOf(resource.getId())))
                .map(SysResource::getResourceName)
                .filter(StringUtils::hasText)
                .toList();
        return names.isEmpty() ? "无权限" : String.join("、", names);
    }

    /**
     * 按条件过滤资源。
     *
     * <p>如果命中的是子节点，这里会把它的祖先节点也一起保留，
     * 否则最终组装树时会出现“子节点还在，但父节点丢了”的问题。
     *
     * @param resources 原始资源列表
     * @param resourceType 资源类型筛选条件
     * @param resourceName 资源名称模糊查询条件
     * @return 过滤后的资源列表
     */
    private List<SysResource> filterResources(List<SysResource> resources, Integer resourceType, String resourceName) {
        String normalizedResourceName = StringUtils.hasText(resourceName) ? resourceName.trim() : null;
        if (resourceType == null && normalizedResourceName == null) {
            return resources;
        }
        // resourceMap 用来在命中子节点时，顺着 parentId 一路把祖先节点也找出来。
        Map<Long, SysResource> resourceMap = resources.stream()
                .collect(Collectors.toMap(SysResource::getId, Function.identity()));
        Set<Long> keepIds = new LinkedHashSet<>();
        for (SysResource resource : resources) {
            boolean matchesType = resourceType == null || Objects.equals(resource.getResourceType(), resourceType);
            boolean matchesName = normalizedResourceName == null
                    || (resource.getResourceName() != null && resource.getResourceName().contains(normalizedResourceName));
            if (matchesType && matchesName) {
                SysResource current = resource;
                while (current != null) {
                    keepIds.add(current.getId());
                    current = current.getParentId() == null ? null : resourceMap.get(current.getParentId());
                }
            }
        }
        return resources.stream().filter(item -> keepIds.contains(item.getId())).toList();
    }

    /**
     * 递归构建资源树。
     *
     * @param resources 资源平铺列表
     * @param parentId 当前层父节点 ID，根节点时为 null
     * @return 当前层级的树节点列表
     */
    private List<Map<String, Object>> buildResourceTree(List<SysResource> resources, Long parentId) {
        return resources.stream()
                .filter(resource -> Objects.equals(resource.getParentId(), parentId))
                .sorted(Comparator.comparing((SysResource item) -> item.getSortNo() == null ? 0 : item.getSortNo())
                        .thenComparing(SysResource::getId))
                // 递归调用自己，把当前资源的子节点继续组装成树。
                .map(resource -> toResourceItem(resource, buildResourceTree(resources, resource.getId())))
                .toList();
    }

    /**
     * 把用户实体转换成前端需要的个人资料结构。
     *
     * @param user 用户实体
     * @param roles 用户角色列表
     * @return 个人资料结构
     */
    private Map<String, Object> toUserProfile(SysUser user, List<SysRole> roles) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("email", user.getEmail());
        result.put("mobile", user.getMobile());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("roleIds", roles.stream().map(SysRole::getId).toList());
        result.put("roleNames", roles.stream().map(SysRole::getRoleName).distinct().toList());
        result.put("status", user.getStatus());
        result.put("lastLoginTime", user.getLastLoginTime());
        result.put("createTime", user.getCreateTime());
        result.put("updateTime", user.getUpdateTime());
        result.put("remark", user.getRemark());
        return result;
    }

    /**
     * 把用户转换成用户列表项。
     *
     * <p>当前列表项结构和个人资料结构一致，所以这里直接复用 toUserProfile。
     *
     * @param user 用户实体
     * @param roles 用户角色列表
     * @return 用户列表项数据
     */
    private Map<String, Object> toUserPageItem(SysUser user, List<SysRole> roles) {
        return toUserProfile(user, roles);
    }

    /**
     * 把角色实体转换成角色列表项。
     *
     * @param role 角色实体
     * @param userCount 角色下用户数量
     * @param permissions 权限摘要字符串
     * @return 角色列表项数据
     */
    private Map<String, Object> toRolePageItem(SysRole role, long userCount, String permissions) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", role.getId());
        result.put("roleCode", role.getRoleCode());
        result.put("roleName", role.getRoleName());
        result.put("description", role.getDescription());
        result.put("remark", role.getRemark());
        result.put("status", role.getStatus());
        result.put("userCount", userCount);
        result.put("permissions", permissions);
        result.put("createTime", role.getCreateTime());
        result.put("updateTime", role.getUpdateTime());
        return result;
    }

    /**
     * 把资源实体转换成资源树节点。
     *
     * @param resource 资源实体
     * @param children 子节点列表
     * @return 资源树节点数据
     */
    private Map<String, Object> toResourceItem(SysResource resource, List<Map<String, Object>> children) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", resource.getId());
        result.put("parentId", resource.getParentId());
        result.put("resourceName", resource.getResourceName());
        result.put("resourceCode", resource.getResourceCode());
        result.put("resourceType", resource.getResourceType());
        result.put("path", resource.getPath());
        result.put("component", resource.getComponent());
        result.put("permissionKey", resource.getPermissionKey());
        result.put("icon", resource.getIcon());
        result.put("sortNo", resource.getSortNo());
        result.put("remark", resource.getRemark());
        result.put("status", resource.getStatus());
        result.put("children", children);
        return result;
    }

    /**
     * 安全处理页码。
     *
     * @param pageNum 原始页码
     * @return 合法页码；如果传空或小于 1，就返回 1
     */
    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 安全处理分页大小。
     *
     * @param pageSize 原始分页大小
     * @return 合法分页大小；如果传空或小于 1，就返回默认值 10
     */
    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    /**
     * 规范化可为空字符串。
     *
     * <p>当前端传入空串或只有空格时，这里会统一转换成 null，
     * 避免数据库里出现很多“看起来有值、其实没意义”的空白字符串。
     *
     * @param value 原始字符串
     * @return 去掉首尾空格后的字符串；如果没有有效内容则返回 null
     */
    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 获取文件扩展名。
     *
     * @param fileName 原始文件名
     * @return 小写扩展名，不包含点号
     */
    private String getFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new BusinessException("头像文件格式不正确");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
