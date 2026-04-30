package com.rpa.manage.controller.data;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.api.Result;
import com.rpa.manage.domain.dto.data.BusinessDataPageQuery;
import com.rpa.manage.domain.dto.data.DataAnalysisPageQuery;
import com.rpa.manage.domain.dto.data.DataCollectionCreateRequest;
import com.rpa.manage.domain.dto.data.DataCollectionPageQuery;
import com.rpa.manage.domain.dto.data.DataProcessingPageQuery;
import com.rpa.manage.service.data.DataService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据管理控制器。
 *
 * <p>这个类覆盖了数据链路里的四个阶段：
 * 1. 数据采集；
 * 2. 数据解析；
 * 3. 数据加工；
 * 4. 最终业务数据查询。
 *
 * <p>因为这四部分都属于“数据结果展示/维护”，所以统一放在一个控制器里对外提供接口。
 */
@Tag(name = "数据管理", description = "数据采集、数据解析、数据加工、数据查询相关接口")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    /*
     * ==================== 数据采集 ====================
     */

    /**
     * 分页查询数据采集记录。
     *
     * @param query 查询条件
     * @return 采集记录分页结果及汇总信息
     */
    @Operation(
            summary = "数据采集分页查询",
            description = "按任务ID、关键字、状态、采集时间范围分页查询采集记录，返回统计汇总、分页信息和列表数据。"
    )
    @GetMapping("/api/v1/data-collection/page")
    @PreAuthorize("hasAuthority('data:collection:page')")
    public Result<Map<String, Object>> collectionPage(DataCollectionPageQuery query) {
        return Result.success(dataService.collectionPage(query));
    }

    /**
     * 查询单条数据采集详情。
     *
     * @param id 采集记录 ID
     * @return 采集详情
     */
    @Operation(
            summary = "数据采集详情",
            description = "根据采集记录ID查询采集详情，返回任务信息、企业信息、采集状态、原始采集数据和时间信息。"
    )
    @GetMapping("/api/v1/data-collection/{id}")
    @PreAuthorize("hasAuthority('data:collection:view')")
    public Result<Map<String, Object>> collectionDetail(
            @Parameter(description = "采集记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.collectionDetail(id));
    }

    /**
     * 手动新增数据采集记录。
     *
     * @param request 采集新增请求
     * @return 新建后的采集记录
     */
    @Operation(
            summary = "新增数据采集记录",
            description = "手动新增一条采集记录，系统会自动绑定该任务最近一次执行记录。"
    )
    @PostMapping("/api/v1/data-collection")
    @PreAuthorize("hasAuthority('data:collection:create')")
    public Result<Map<String, Object>> createCollection(@Valid @RequestBody DataCollectionCreateRequest request) {
        return Result.success(dataService.createCollection(request));
    }

    /**
     * 删除指定数据采集记录。
     *
     * @param id 采集记录 ID
     * @return 删除结果
     */
    @Operation(
            summary = "删除数据采集记录",
            description = "根据采集记录ID物理删除采集数据。"
    )
    @DeleteMapping("/api/v1/data-collection/{id}")
    @PreAuthorize("hasAuthority('data:collection:delete')")
    public Result<Map<String, Object>> deleteCollection(
            @Parameter(description = "采集记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.deleteCollection(id));
    }

    /*
     * ==================== 数据解析 ====================
     */

    /**
     * 分页查询数据解析记录。
     *
     * @param query 查询条件
     * @return 解析记录分页结果及汇总信息
     */
    @Operation(
            summary = "数据解析分页查询",
            description = "按任务ID、状态、解析时间范围分页查询解析记录，返回统计汇总、分页信息和列表数据。"
    )
    @GetMapping("/api/v1/data-analysis/page")
    @PreAuthorize("hasAuthority('data:analysis:page')")
    public Result<Map<String, Object>> analysisPage(DataAnalysisPageQuery query) {
        return Result.success(dataService.analysisPage(query));
    }

    /**
     * 查询单条数据解析详情。
     *
     * @param id 解析记录 ID
     * @return 解析详情
     */
    @Operation(
            summary = "数据解析详情",
            description = "根据解析记录ID查询解析详情，返回任务信息、采集关联信息、解析结果、错误信息和时间信息。"
    )
    @GetMapping("/api/v1/data-analysis/{id}")
    @PreAuthorize("hasAuthority('data:analysis:view')")
    public Result<Map<String, Object>> analysisDetail(
            @Parameter(description = "解析记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.analysisDetail(id));
    }

    /**
     * 删除指定数据解析记录。
     *
     * @param id 解析记录 ID
     * @return 删除结果
     */
    @Operation(
            summary = "删除数据解析记录",
            description = "根据解析记录ID物理删除解析数据。"
    )
    @DeleteMapping("/api/v1/data-analysis/{id}")
    @PreAuthorize("hasAuthority('data:analysis:delete')")
    public Result<Map<String, Object>> deleteAnalysis(
            @Parameter(description = "解析记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.deleteAnalysis(id));
    }

    /*
     * ==================== 数据加工 ====================
     */

    /**
     * 分页查询数据加工记录。
     *
     * @param query 查询条件
     * @return 加工记录分页结果及汇总信息
     */
    @Operation(
            summary = "数据加工分页查询",
            description = "按任务ID、状态、加工时间范围分页查询加工记录，返回统计汇总、分页信息和列表数据。"
    )
    @GetMapping("/api/v1/data-processing/page")
    @PreAuthorize("hasAuthority('data:processing:page')")
    public Result<Map<String, Object>> processingPage(DataProcessingPageQuery query) {
        return Result.success(dataService.processingPage(query));
    }

    /**
     * 查询单条数据加工详情。
     *
     * @param id 加工记录 ID
     * @return 加工详情
     */
    @Operation(
            summary = "数据加工详情",
            description = "根据加工记录ID查询加工详情，返回任务信息、解析关联信息、加工结果、验证结果和时间信息。"
    )
    @GetMapping("/api/v1/data-processing/{id}")
    @PreAuthorize("hasAuthority('data:processing:view')")
    public Result<Map<String, Object>> processingDetail(
            @Parameter(description = "加工记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.processingDetail(id));
    }

    /**
     * 删除指定数据加工记录。
     *
     * @param id 加工记录 ID
     * @return 删除结果
     */
    @Operation(
            summary = "删除数据加工记录",
            description = "根据加工记录ID物理删除加工数据。"
    )
    @DeleteMapping("/api/v1/data-processing/{id}")
    @PreAuthorize("hasAuthority('data:processing:delete')")
    public Result<Map<String, Object>> deleteProcessing(
            @Parameter(description = "加工记录ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.deleteProcessing(id));
    }

    /*
     * ==================== 业务数据 ====================
     */

    /**
     * 分页查询最终业务数据。
     *
     * @param query 查询条件
     * @return 业务数据分页结果
     */
    @Operation(
            summary = "业务数据分页查询",
            description = "按关键字、任务ID、税区ID、数据状态、创建时间范围分页查询最终业务数据。"
    )
    @GetMapping("/api/v1/business-data/page")
    @PreAuthorize("hasAuthority('data:business:page')")
    public Result<PageResult<Map<String, Object>>> businessPage(BusinessDataPageQuery query) {
        return Result.success(dataService.businessPage(query));
    }

    /**
     * 查询业务数据详情。
     *
     * @param id 业务数据 ID
     * @return 业务数据详情
     */
    @Operation(
            summary = "业务数据详情",
            description = "根据业务数据ID查询最终业务数据详情，返回任务信息、企业信息、税区信息和业务数据内容。"
    )
    @GetMapping("/api/v1/business-data/{id}")
    @PreAuthorize("hasAuthority('data:business:view')")
    public Result<Map<String, Object>> businessDetail(
            @Parameter(description = "业务数据ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.businessDetail(id));
    }

    /**
     * 删除最终业务数据。
     *
     * @param id 业务数据 ID
     * @return 删除结果
     */
    @Operation(
            summary = "删除业务数据",
            description = "根据业务数据ID物理删除最终业务数据。"
    )
    @DeleteMapping("/api/v1/business-data/{id}")
    @PreAuthorize("hasAuthority('data:business:delete')")
    public Result<Map<String, Object>> deleteBusiness(
            @Parameter(description = "业务数据ID", example = "1") @PathVariable Long id
    ) {
        return Result.success(dataService.deleteBusiness(id));
    }
}
