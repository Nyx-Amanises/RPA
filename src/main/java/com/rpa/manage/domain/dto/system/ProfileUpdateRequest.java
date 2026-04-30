package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 个人信息更新请求。
 */
@Data
@Schema(description = "个人信息更新请求")
public class ProfileUpdateRequest {

    @Schema(description = "真实姓名", example = "系统管理员")
    @NotBlank(message = "姓名不能为空")
    private String realName;

    @Schema(description = "邮箱", example = "admin@rpa.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    private String mobile;
}
