package com.rpa.manage.controller.auth;

import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.auth.LoginRequest;
import com.rpa.manage.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证模块控制器。
 *
 * <p>Controller 这一层尽量保持轻量：
 * 负责接收前端请求、做参数校验、调用服务层，并把结果包装成统一响应格式。
 * 真正的登录校验和权限数据组装逻辑，仍然在 {@link AuthService} 里完成。
 */
@Tag(name = "认证管理", description = "登录认证与当前登录信息接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录。
     *
     * <p>前端调用这个接口成功后，通常会拿到 token、token 前缀，以及一个简化版的用户信息对象，
     * 用来初始化页面头部、权限按钮和动态菜单。
     *
     * @param request 登录请求参数
     * @return 统一响应体，data 中包含 token 和登录用户信息
     */
    @Operation(
            summary = "用户登录",
            description = "校验用户名和密码，登录成功后返回 token 与登录用户信息",
            responses = {
                    @ApiResponse(responseCode = "200", description = "登录成功"),
                    @ApiResponse(responseCode = "400", description = "请求参数错误", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    /**
     * 获取当前登录用户信息。
     *
     * <p>当前端刷新页面、重新进入系统，或者打开个人中心时，
     * 通常会调用这个接口来重新获取最新用户资料。
     *
     * @return 统一响应体，data 中包含当前用户详细信息
     */
    @Operation(summary = "获取当前登录用户信息", description = "返回当前登录用户的基础资料、角色名称、头像、状态和时间字段")
    @GetMapping("/current-user")
    public Result<Map<String, Object>> currentUser() {
        return Result.success(authService.currentUser());
    }
}
