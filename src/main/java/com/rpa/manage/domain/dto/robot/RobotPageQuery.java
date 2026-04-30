package com.rpa.manage.domain.dto.robot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "机器人分页查询参数")
public class RobotPageQuery {

    @Schema(description = "机器人名称，支持模糊查询", example = "机器人A001")
    private String robotName;

    @Schema(description = "机器人编码，支持模糊查询", example = "ROBOT_001")
    private String robotCode;

    @Schema(description = "机器人状态：0离线，1在线，2工作中", example = "1")
    private Integer status;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;
}
