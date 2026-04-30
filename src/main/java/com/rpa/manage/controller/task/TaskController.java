package com.rpa.manage.controller.task;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.task.TaskPageQuery;
import com.rpa.manage.domain.dto.task.TaskUpsertRequest;
import com.rpa.manage.service.task.TaskService;
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
 * 任务管理控制器。
 *
 * <p>这个控制器负责任务的完整生命周期：
 * 查询、创建、查看详情、修改、手动执行、删除。
 *
 * <p>你可以把“任务”理解成一个业务执行单元，它会关联流程和机器人，
 * 所以这里的接口很多时候也是前端任务管理页面的主入口。
 */
@Tag(name = "任务管理", description = "任务列表、详情、新增、修改、执行、删除等接口")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * 分页查询任务列表。
     *
     * @param query 查询条件
     * @return 任务分页结果
     */
    @Operation(summary = "任务分页查询", description = "分页查询任务列表，支持按任务编码/名称、状态、创建时间筛选。")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('task:page')")
    public Result<PageResult<Map<String, Object>>> page(TaskPageQuery query) {
        return Result.success(taskService.page(query));
    }

    /**
     * 新增任务。
     *
     * @param request 任务新增请求
     * @return 新建后的任务信息
     */
    @Operation(summary = "新增任务", description = "新增任务并绑定流程、机器人，任务编码由后端自动生成。")
    @PostMapping
    @PreAuthorize("hasAuthority('task:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody TaskUpsertRequest request) {
        return Result.success(taskService.create(request));
    }

    /**
     * 查询任务详情。
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    @Operation(summary = "任务详情", description = "根据任务ID查询任务基础信息及最近一次执行摘要。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('task:view')")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(taskService.detail(id));
    }

    /**
     * 修改指定任务。
     *
     * @param id 任务 ID
     * @param request 任务修改请求
     * @return 修改后的任务信息
     */
    @Operation(summary = "修改任务", description = "修改任务基础信息，任务编码创建后不允许修改。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('task:update')")
    public Result<Map<String, Object>> update(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody TaskUpsertRequest request
    ) {
        return Result.success(taskService.update(id, request));
    }

    /**
     * 手动触发任务执行。
     *
     * @param id 任务 ID
     * @return 执行结果摘要
     */
    @Operation(summary = "执行任务", description = "手动触发任务异步执行，接口返回排队中的执行记录，后台线程按流程步骤顺序执行。")
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<Map<String, Object>> execute(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(taskService.execute(id));
    }

    /**
     * 删除指定任务。
     *
     * @param id 任务 ID
     * @return 删除结果
     */
    @Operation(summary = "删除任务", description = "删除指定任务，执行中的任务不允许删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('task:delete')")
    public Result<Map<String, Object>> delete(
            @Parameter(description = "任务ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(taskService.delete(id));
    }
}
