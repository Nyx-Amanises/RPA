package com.rpa.manage.controller.robot;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.robot.RobotPageQuery;
import com.rpa.manage.domain.dto.robot.RobotUpsertRequest;
import com.rpa.manage.service.robot.RobotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 机器人管理控制器。
 *
 * <p>机器人模块主要维护可参与 RPA 执行的机器人实体，
 * 包括统计概览、列表管理、详情查看和增删改。
 *
 * <p>Controller 这里仍然保持轻量，复杂校验和删除限制等规则在服务层完成。
 */
@Tag(name = "机器人管理", description = "机器人列表、详情、新增、修改、删除等接口")
@RestController
@RequestMapping("/api/v1/robots")
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;

    /**
     * 获取机器人概览统计。
     *
     * @return 机器人统计信息
     */
    @Operation(summary = "机器人统计信息", description = "返回机器人总数、在线数、工作中数、离线数，供页面顶部统计卡片展示。")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('robot:overview')")
    public Result<Map<String, Object>> overview() {
        return Result.success(robotService.overview());
    }

    /**
     * 分页查询机器人列表。
     *
     * @param query 查询条件
     * @return 机器人分页结果
     */
    @Operation(summary = "机器人分页查询", description = "分页查询机器人列表，支持按机器人名称、编码、状态筛选。")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('robot:page')")
    public Result<PageResult<Map<String, Object>>> page(RobotPageQuery query) {
        return Result.success(robotService.page(query));
    }

    /**
     * 新增机器人。
     *
     * @param request 机器人新增请求
     * @return 新建后的机器人信息
     */
    @Operation(summary = "新增机器人", description = "新增机器人基础信息。")
    @PostMapping
    @PreAuthorize("hasAuthority('robot:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody RobotUpsertRequest request) {
        return Result.success(robotService.create(request));
    }

    /**
     * 查询机器人详情。
     *
     * @param id 机器人 ID
     * @return 机器人详情
     */
    @Operation(summary = "机器人详情", description = "根据机器人 ID 查询机器人详情。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('robot:view')")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "机器人ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(robotService.detail(id));
    }

    /**
     * 修改指定机器人。
     *
     * @param id 机器人 ID
     * @param request 机器人修改请求
     * @return 修改后的机器人信息
     */
    @Operation(summary = "修改机器人", description = "修改机器人基础信息和状态。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('robot:update')")
    public Result<Map<String, Object>> update(
            @Parameter(description = "机器人ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody RobotUpsertRequest request
    ) {
        return Result.success(robotService.update(id, request));
    }

    /**
     * 删除指定机器人。
     *
     * @param id 机器人 ID
     * @return 删除结果
     */
    @Operation(summary = "删除机器人", description = "删除指定机器人；如果机器人正在工作或已被任务绑定，则不允许删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('robot:delete')")
    public Result<Map<String, Object>> delete(
            @Parameter(description = "机器人ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(robotService.delete(id));
    }
}
