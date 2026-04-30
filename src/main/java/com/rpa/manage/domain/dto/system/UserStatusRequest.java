package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态修改请求。
 */
@Data
@Schema(description = "用户状态修改请求")
public class UserStatusRequest {

    @Schema(description = "状态，1 启用，0 禁用", example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
