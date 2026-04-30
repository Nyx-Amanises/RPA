package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求。
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "原密码", example = "123456")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码", example = "12345678")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @Schema(description = "确认新密码", example = "12345678")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
