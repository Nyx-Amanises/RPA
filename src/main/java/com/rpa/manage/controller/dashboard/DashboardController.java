package com.rpa.manage.controller.dashboard;

import com.rpa.manage.common.api.Result;
import com.rpa.manage.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页模块控制器。
 *
 * <p>这一层主要给前端首页提供“开箱即用”的展示数据，
 * 例如统计卡片、概览信息、最近任务列表等。
 *
 * <p>因为首页更偏展示，所以接口数量不多，但通常会在用户登录后第一时间被调用。
 */
@Tag(name = "首页模块", description = "首页统计卡片、任务状态概览和最近任务列表相关接口")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取首页汇总统计信息。
     *
     * @return 首页汇总数据
     */
    @Operation(summary = "首页汇总统计", description = "返回首页统计卡片、任务状态概览以及系统信息等汇总数据。")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<Map<String, Object>> summary() {
        return Result.success(dashboardService.summary());
    }

    /**
     * 获取最近任务列表。
     *
     * @param limit 返回条数
     * @return 最近任务列表
     */
    @Operation(summary = "获取最近任务列表", description = "按任务创建时间倒序查询最近任务，默认返回 5 条，可通过 limit 控制返回条数。")
    @GetMapping("/recent-tasks")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<List<Map<String, Object>>> recentTasks(
            @Parameter(description = "返回最近任务条数，建议范围 5-10，默认 5", example = "5")
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        return Result.success(dashboardService.recentTasks(limit));
    }
}
