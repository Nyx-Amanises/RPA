package com.rpa.manage.controller.execution;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.task.ExecutionPageQuery;
import com.rpa.manage.service.execution.ExecutionLogStreamService;
import com.rpa.manage.service.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 执行记录控制器。
 *
 * <p>执行记录本质上属于任务模块的运行结果，因此这里虽然单独拆成了控制器，
 * 但底层仍然复用了 {@link TaskService} 中和执行记录相关的方法。
 *
 * <p>前端通常会用这些接口做执行历史列表、执行详情弹窗、日志查看等功能。
 */
@Tag(name = "执行记录", description = "执行记录列表与详情接口")
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final TaskService taskService;
    private final ExecutionLogStreamService executionLogStreamService;

    /**
     * 分页查询执行记录。
     *
     * @param query 查询条件
     * @return 执行记录分页结果
     */
    @Operation(summary = "执行记录分页查询", description = "分页查询执行记录，支持按任务ID、执行状态、执行时间筛选。")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('execution:page')")
    public Result<PageResult<Map<String, Object>>> page(ExecutionPageQuery query) {
        return Result.success(taskService.executionPage(query));
    }

    /**
     * 查询执行记录详情。
     *
     * @param id 执行记录 ID
     * @return 执行详情
     */
    @Operation(summary = "执行记录详情", description = "根据执行记录ID查询执行详情、步骤日志和错误信息。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('execution:view')")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "执行记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(taskService.executionDetail(id));
    }

    @Operation(summary = "执行日志实时流", description = "通过 SSE 持续推送指定执行记录的运行状态、步骤日志、失败堆栈和截图地址。")
    @GetMapping(value = "/{id}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('execution:view')")
    public SseEmitter streamLogs(
            @Parameter(description = "执行记录ID", example = "1") @PathVariable Long id
    ) {
        return executionLogStreamService.subscribe(id);
    }
}
