package com.rpa.manage.security;

import com.rpa.manage.domain.entity.SysRole;
import com.rpa.manage.domain.entity.SysUser;
import com.rpa.manage.domain.repository.SysUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 鉴权过滤器。
 *
 * <p>它会在每次请求进入控制器之前执行一次，核心职责是：
 * 1. 从请求头中提取 Bearer Token；
 * 2. 校验 token 是否存在、是否有效；
 * 3. 根据 token 中的用户 ID 重新加载用户和权限；
 * 4. 把认证结果放进 Spring Security 上下文。
 *
 * <p>因为它继承的是 {@link OncePerRequestFilter}，所以同一次请求只会执行一次，
 * 比较适合做这种统一登录态校验。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /*
     * 这些依赖分别负责解析 token、查询用户、计算权限，以及维护服务端登录会话。
     */
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserRepository sysUserRepository;
    private final PermissionService permissionService;
    private final LoginSessionService loginSessionService;

    /**
     * 对进入系统的请求执行 JWT 鉴权。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            // 没带 token 的请求直接放行，后面会由 Spring Security 判断它是否允许匿名访问。
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        try {
            if (!loginSessionService.exists(token)) {
                // token 在服务端会话中已失效时，主动清理上下文，避免脏认证信息残留。
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtTokenProvider.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            SysUser user = sysUserRepository.findById(userId).orElse(null);
            if (user != null && user.getStatus() == 1) {
                // 每次请求都重新计算角色和权限，确保角色变更后无需重新发 token 也能尽快生效。
                List<SysRole> roles = permissionService.loadEffectiveRoles(user);
                SysRole role = roles.isEmpty() ? null : roles.get(0);
                List<String> permissions = permissionService.loadPermissions(user);
                LoginUser loginUser = LoginUser.builder()
                        .userId(user.getId())
                        .roleId(role == null ? null : role.getId())
                        .roleCode(role == null ? null : role.getRoleCode())
                        .roleName(role == null ? null : role.getRoleName())
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .status(user.getStatus())
                        .permissions(permissions)
                        .build();
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                // 请求活跃时顺便刷新会话有效期，避免用户操作过程中被意外踢下线。
                loginSessionService.refresh(token);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // token 非法、过期或解析失败时，统一当作未登录处理。
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
