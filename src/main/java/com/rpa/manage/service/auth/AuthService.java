package com.rpa.manage.service.auth;

import com.rpa.manage.domain.dto.auth.LoginRequest;
import java.util.Map;

/**
 * 身份认证模块服务接口。
 *
 * <p>这里仅定义认证模块对 Controller 暴露的能力边界，
 * 具体实现放到 {@code impl} 包中，便于后续替换真实登录逻辑。
 */
public interface AuthService {

    /**
     * 登录接口服务定义。
     *
     * @param request 登录请求参数
     * @return 登录成功后的响应数据
     */
    Map<String, Object> login(LoginRequest request);

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户信息
     */
    Map<String, Object> currentUser();
}
