package com.rpa.manage.security;

import com.rpa.manage.common.api.Result;
import com.rpa.manage.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 未登录响应处理器。
 *
 * <p>当请求没有通过认证，例如没有带 token、token 失效、token 非法时，
 * Spring Security 会回调这里，统一返回 401 响应。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 处理未登录请求。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authException 认证异常
     * @throws IOException 输出响应时的异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.failure(ResultCode.UNAUTHORIZED));
    }
}
