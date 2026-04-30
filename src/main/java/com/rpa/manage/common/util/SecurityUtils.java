package com.rpa.manage.common.util;

import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类。
 *
 * <p>这个工具类用于从 Spring Security 上下文中提取当前登录用户信息，
 * 避免在业务代码里重复写同样的获取逻辑。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户对象。
     *
     * @return 当前登录用户
     */
    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException("未获取到当前登录用户信息");
        }
        return loginUser;
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前登录用户 ID
     */
    public static Long currentUserId() {
        return currentUser().getUserId();
    }
}
