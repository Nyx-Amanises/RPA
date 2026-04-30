package com.rpa.manage.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
@Schema(description = "登录请求参数")
public class LoginRequest {

    @Schema(description = "登录用户名", example = "admin")
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 20, message = "登录账号长度不能超过20")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "登录密码不能为空")
    @Size(max = 20, message = "登录密码长度不能超过20")
    private String password;
}
