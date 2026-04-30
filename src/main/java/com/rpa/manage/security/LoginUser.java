package com.rpa.manage.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 登录用户模型。
 *
 * <p>这个类实现了 Spring Security 的 {@link UserDetails} 接口，
 * 作用是把我们项目里的用户信息，转换成安全框架认识的“登录态对象”。
 *
 * <p>它通常会在两个地方出现：
 * 1. 用户登录成功后，被写进 token / 登录会话；
 * 2. 后续请求经过 JWT 过滤器时，被放入 SecurityContext。
 */
@Getter
@Builder
public class LoginUser implements UserDetails {

    /**
     * 用户主键 ID。
     */
    private final Long userId;
    /**
     * 主角色 ID。
     */
    private final Long roleId;
    /**
     * 主角色编码。
     */
    private final String roleCode;
    /**
     * 主角色名称。
     */
    private final String roleName;
    /**
     * 登录用户名。
     */
    private final String username;
    /**
     * 已加密的密码。
     */
    private final String password;
    /**
     * 用户状态，通常 1 表示启用。
     */
    private final Integer status;
    /**
     * 用户拥有的权限标识集合。
     */
    @Builder.Default
    private final List<String> permissions = List.of();

    /**
     * 把角色和权限转换成 Spring Security 能识别的授权集合。
     *
     * <p>这里会同时加入：
     * 1. `ROLE_xxx` 形式的角色权限；
     * 2. 具体的按钮/页面权限字符串。
     *
     * @return 授权信息集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        if (roleCode != null && !roleCode.isBlank()) {
            // Spring Security 对角色通常约定使用 ROLE_ 前缀。
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return List.copyOf(authorities);
    }

    /**
     * 账号是否未过期。
     *
     * @return 当前实现固定返回 true，表示暂未启用“账号过期”能力
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定。
     *
     * @return 当前实现固定返回 true，表示暂未启用“账号锁定”能力
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭证是否未过期。
     *
     * @return 当前实现固定返回 true，表示暂未启用“密码过期”能力
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 用户是否启用。
     *
     * <p>这里直接根据数据库里的状态字段判断，只有状态为 1 才允许视为可用账号。
     *
     * @return true 表示启用
     */
    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
