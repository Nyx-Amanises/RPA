package com.rpa.manage.controller.indicator;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.indicator.IndicatorPageQuery;
import com.rpa.manage.domain.dto.indicator.IndicatorUpsertRequest;
import com.rpa.manage.domain.dto.indicator.QuotaCalculateRequest;
import com.rpa.manage.domain.dto.indicator.QuotaRulePageQuery;
import com.rpa.manage.domain.dto.indicator.QuotaRuleUpsertRequest;
import com.rpa.manage.service.indicator.IndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@Tag(name = "指标管理", description = "指标计算和指标额度计算相关接口")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;

    @Operation(summary = "指标定义分页查询")
    @GetMapping("/indicators/page")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<Map<String, Object>>> indicatorPage(IndicatorPageQuery query) {
        return Result.success(indicatorService.indicatorPage(query));
    }

    @Operation(summary = "指标选项列表")
    @GetMapping("/indicators/options")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> indicatorOptions() {
        return Result.success(indicatorService.indicatorOptions());
    }

    @Operation(summary = "指标详情")
    @GetMapping("/indicators/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> indicatorDetail(
            @Parameter(description = "指标ID") @PathVariable Long id
    ) {
        return Result.success(indicatorService.indicatorDetail(id));
    }

    @Operation(summary = "新增指标")
    @PostMapping("/indicators")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> createIndicator(@Valid @RequestBody IndicatorUpsertRequest request) {
        return Result.success(indicatorService.createIndicator(request));
    }

    @Operation(summary = "编辑指标")
    @PutMapping("/indicators/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> updateIndicator(
            @Parameter(description = "指标ID") @PathVariable Long id,
            @Valid @RequestBody IndicatorUpsertRequest request
    ) {
        return Result.success(indicatorService.updateIndicator(id, request));
    }

    @Operation(summary = "删除指标")
    @DeleteMapping("/indicators/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> deleteIndicator(
            @Parameter(description = "指标ID") @PathVariable Long id
    ) {
        return Result.success(indicatorService.deleteIndicator(id));
    }

    @Operation(summary = "触发指标计算")
    @PostMapping("/indicators/{id}/calculate")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> calculateIndicator(
            @Parameter(description = "指标ID") @PathVariable Long id
    ) {
        return Result.success(indicatorService.calculateIndicator(id));
    }

    @Operation(summary = "指标额度计算规则分页查询")
    @GetMapping("/indicator-quotas/page")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<Map<String, Object>>> quotaRulePage(QuotaRulePageQuery query) {
        return Result.success(indicatorService.quotaRulePage(query));
    }

    @Operation(summary = "指标额度计算规则详情")
    @GetMapping("/indicator-quotas/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> quotaRuleDetail(
            @Parameter(description = "额度计算规则ID") @PathVariable Long id
    ) {
        return Result.success(indicatorService.quotaRuleDetail(id));
    }

    @Operation(summary = "新增指标额度计算规则")
    @PostMapping("/indicator-quotas")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> createQuotaRule(@Valid @RequestBody QuotaRuleUpsertRequest request) {
        return Result.success(indicatorService.createQuotaRule(request));
    }

    @Operation(summary = "编辑指标额度计算规则")
    @PutMapping("/indicator-quotas/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> updateQuotaRule(
            @Parameter(description = "额度计算规则ID") @PathVariable Long id,
            @Valid @RequestBody QuotaRuleUpsertRequest request
    ) {
        return Result.success(indicatorService.updateQuotaRule(id, request));
    }

    @Operation(summary = "删除指标额度计算规则")
    @DeleteMapping("/indicator-quotas/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> deleteQuotaRule(
            @Parameter(description = "额度计算规则ID") @PathVariable Long id
    ) {
        return Result.success(indicatorService.deleteQuotaRule(id));
    }

    @Operation(summary = "触发指标额度计算")
    @PostMapping("/indicator-quotas/{id}/calculate")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> calculateQuotaRule(
            @Parameter(description = "额度计算规则ID") @PathVariable Long id,
            @RequestBody(required = false) QuotaCalculateRequest request
    ) {
        return Result.success(indicatorService.calculateQuotaRule(id, request));
    }
}
