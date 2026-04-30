package com.rpa.manage.domain.dto.system;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 角色资源分配请求。
 */
@Data
@Schema(description = "角色资源分配请求")
public class RoleResourceAssignRequest {

    @ArraySchema(schema = @Schema(description = "资源 ID", example = "28"), arraySchema = @Schema(description = "需要分配给角色的资源 ID 列表"))
    @NotEmpty(message = "资源 ID 列表不能为空")
    private List<Long> resourceIds;
}
