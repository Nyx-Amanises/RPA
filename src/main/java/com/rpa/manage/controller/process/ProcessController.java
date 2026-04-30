package com.rpa.manage.controller.process;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.process.ProcessDesignRequest;
import com.rpa.manage.domain.dto.process.ProcessPageQuery;
import com.rpa.manage.domain.dto.process.ProcessUpsertRequest;
import com.rpa.manage.service.process.ProcessService;
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
 * 流程管理控制器。
 *
 * <p>流程模块负责维护任务执行步骤模板，
 * 所以这里既有流程基础信息接口，也有流程设计接口。
 *
 * <p>对前端来说，流程列表页、流程详情页、流程设计页，
 * 基本都要通过这个控制器访问后端。
 */
@Tag(name = "流程管理", description = "流程列表、详情、新增、修改、设计、删除等接口")
@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    /**
     * 分页查询流程列表。
     *
     * @param query 查询条件
     * @return 流程分页结果
     */
    @Operation(summary = "流程分页查询", description = "分页查询流程列表，支持按流程名称、流程编码、状态筛选。")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('process:page')")
    public Result<PageResult<Map<String, Object>>> page(ProcessPageQuery query) {
        return Result.success(processService.page(query));
    }

    /**
     * 新增流程。
     *
     * @param request 流程新增请求
     * @return 新建后的流程信息
     */
    @Operation(summary = "新增流程", description = "新增流程基础信息。")
    @PostMapping
    @PreAuthorize("hasAuthority('process:create')")
    public Result<Map<String, Object>> create(@Valid @RequestBody ProcessUpsertRequest request) {
        return Result.success(processService.create(request));
    }

    /**
     * 查询流程基础详情。
     *
     * @param id 流程 ID
     * @return 流程详情
     */
    @Operation(summary = "流程详情", description = "根据流程ID查询流程基础信息。")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('process:view')")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(processService.detail(id));
    }

    /**
     * 查询流程设计详情。
     *
     * @param id 流程 ID
     * @return 流程设计数据
     */
    @Operation(summary = "流程设计详情", description = "根据流程ID查询流程设计步骤，供流程设计页面刷新回显使用。")
    @GetMapping("/{id}/design")
    @PreAuthorize("hasAuthority('process:design')")
    public Result<Map<String, Object>> designDetail(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(processService.designDetail(id));
    }

    /**
     * 修改指定流程。
     *
     * @param id 流程 ID
     * @param request 流程修改请求
     * @return 修改后的流程信息
     */
    @Operation(summary = "修改流程", description = "修改流程基础信息。流程编码创建后不允许修改。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('process:update')")
    public Result<Map<String, Object>> update(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProcessUpsertRequest request
    ) {
        return Result.success(processService.update(id, request));
    }

    /**
     * 保存流程设计。
     *
     * @param id 流程 ID
     * @param request 流程设计请求
     * @return 保存结果
     */
    @Operation(summary = "保存流程设计", description = "保存流程步骤设计，提交后会覆盖当前流程下原有步骤。")
    @PutMapping("/{id}/design")
    @PreAuthorize("hasAuthority('process:design')")
    public Result<Map<String, Object>> saveDesign(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProcessDesignRequest request
    ) {
        return Result.success(processService.saveDesign(id, request));
    }

    @Operation(summary = "发布流程版本", description = "将当前流程草稿发布为新的可执行版本快照。")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('process:design')")
    public Result<Map<String, Object>> publish(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(processService.publish(id));
    }

    @Operation(summary = "停用流程版本", description = "停用当前已发布版本，停用后任务不能再基于该版本发起执行。")
    @PostMapping("/{id}/disable-version")
    @PreAuthorize("hasAuthority('process:design')")
    public Result<Map<String, Object>> disablePublishedVersion(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(processService.disablePublishedVersion(id));
    }

    /**
     * 删除指定流程。
     *
     * @param id 流程 ID
     * @return 删除结果
     */
    @Operation(summary = "删除流程", description = "删除指定流程；若流程已被任务绑定，则不允许删除。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('process:delete')")
    public Result<Map<String, Object>> delete(
            @Parameter(description = "流程ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(processService.delete(id));
    }
}
