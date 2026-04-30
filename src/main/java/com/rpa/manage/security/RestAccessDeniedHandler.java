package com.rpa.manage.security;

import com.rpa.manage.common.api.Result;
import com.rpa.manage.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 无权限响应处理器。
 *
 * <p>当用户已经登录，但没有访问某个接口所需权限时，
 * Spring Security 会回调这个处理器，统一返回 403 响应。
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 处理无权限访问。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param accessDeniedException 无权限异常
     * @throws IOException 输出响应时的异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.failure(ResultCode.FORBIDDEN));
    }
}
