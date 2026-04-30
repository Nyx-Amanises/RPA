package com.rpa.manage.service.impl.data;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.common.util.JsonUtils;
import com.rpa.manage.common.util.SkeletonSupport;
import com.rpa.manage.domain.dto.data.BusinessDataPageQuery;
import com.rpa.manage.domain.dto.data.DataAnalysisPageQuery;
import com.rpa.manage.domain.dto.data.DataCollectionCreateRequest;
import com.rpa.manage.domain.dto.data.DataCollectionPageQuery;
import com.rpa.manage.domain.dto.data.DataProcessingPageQuery;
import com.rpa.manage.domain.entity.BusinessDataFinal;
import com.rpa.manage.domain.entity.DataAnalysis;
import com.rpa.manage.domain.entity.DataCollection;
import com.rpa.manage.domain.entity.DataProcessing;
import com.rpa.manage.domain.entity.RpaExecutionRecord;
import com.rpa.manage.domain.entity.RpaTask;
import com.rpa.manage.domain.repository.BusinessDataFinalRepository;
import com.rpa.manage.domain.repository.DataAnalysisRepository;
import com.rpa.manage.domain.repository.DataCollectionRepository;
import com.rpa.manage.domain.repository.DataProcessingRepository;
import com.rpa.manage.domain.repository.RpaExecutionRecordRepository;
import com.rpa.manage.domain.repository.RpaTaskRepository;
import com.rpa.manage.service.data.DataService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 数据管理服务实现。
 *
 * <p>这个类主要负责 4 类数据的后台读写与展示组装：
 * 1. 数据采集：保存原始抓取结果；
 * 2. 数据解析：从原始数据里提取结构化字段；
 * 3. 数据加工：对解析结果做清洗、转换和校验；
 * 4. 最终业务数据：提供给业务系统使用的成品数据。
 *
 * <p>整体上它更偏“查询与展示组装”：
 * 一方面负责分页、详情、删除这类接口；
 * 另一方面负责把数据库实体转换成前端更容易直接使用的返回结构。
 */
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    /**
     * 采集阶段状态常量：待处理。
     */
    private static final int COLLECTION_STATUS_PENDING = 0;
    /**
     * 采集阶段状态常量：处理中。
     */
    private static final int COLLECTION_STATUS_PROCESSING = 1;
    /**
     * 采集阶段状态常量：成功。
     */
    private static final int COLLECTION_STATUS_SUCCESS = 2;
    /**
     * 采集阶段状态常量：失败。
     */
    private static final int COLLECTION_STATUS_FAILED = 3;

    /**
     * 解析阶段状态常量：待处理。
     */
    private static final int ANALYSIS_STATUS_PENDING = 0;
    /**
     * 解析阶段状态常量：处理中。
     */
    private static final int ANALYSIS_STATUS_PROCESSING = 1;
    /**
     * 解析阶段状态常量：成功。
     */
    private static final int ANALYSIS_STATUS_SUCCESS = 2;
    /**
     * 解析阶段状态常量：失败。
     */
    private static final int ANALYSIS_STATUS_FAILED = 3;

    /**
     * 加工阶段状态常量：待处理。
     */
    private static final int PROCESSING_STATUS_PENDING = 0;
    /**
     * 加工阶段状态常量：处理中。
     */
    private static final int PROCESSING_STATUS_PROCESSING = 1;
    /**
     * 加工阶段状态常量：成功。
     */
    private static final int PROCESSING_STATUS_SUCCESS = 2;
    /**
     * 加工阶段状态常量：失败。
     */
    private static final int PROCESSING_STATUS_FAILED = 3;

    /**
     * 最终业务数据状态常量：可用。
     */
    private static final int BUSINESS_DATA_STATUS_AVAILABLE = 1;

    /*
     * 下面这些 Repository 分别对应采集、解析、加工、业务数据、任务和执行记录等表。
     * DataServiceImpl 自己不写 SQL，主要负责把这些表的数据查出来，再按接口需要重新组装。
     */
    private final BusinessDataFinalRepository businessDataFinalRepository;
    private final DataAnalysisRepository dataAnalysisRepository;
    private final DataCollectionRepository dataCollectionRepository;
    private final DataProcessingRepository dataProcessingRepository;
    private final RpaTaskRepository rpaTaskRepository;
    private final RpaExecutionRecordRepository rpaExecutionRecordRepository;
    private final ObjectMapper objectMapper;

    /*
     * ==================== 数据采集 ====================
     */

    /**
     * 采集数据分页查询。
     *
     * <p>这个接口主要给“采集列表页”使用，支持按任务、关键字、状态、
     * 采集时间区间筛选，同时会额外返回汇总统计信息，方便前端直接展示顶部统计卡片。
     *
     * @param query 采集数据分页查询条件
     * @return 包含 summary、分页信息和列表数据的结果
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> collectionPage(DataCollectionPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("collectionTime"), Sort.Order.desc("id"))
        );

        Specification<DataCollection> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), query.getTaskId()));
            }
            if (StringUtils.hasText(query.getKeyword())) {
                String keyword = "%" + query.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("taxpayerIdNo"), keyword),
                        cb.like(root.get("enterpriseName"), keyword)
                ));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("collectionTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("collectionTime"), query.getEndTime()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<DataCollection> page = dataCollectionRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toCollectionPageItem)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildCollectionSummary());
        result.put("total", page.getTotalElements());
        result.put("pageNum", page.getNumber() + 1);
        result.put("pageSize", page.getSize());
        result.put("list", list);
        return result;
    }

    /**
     * 查询采集记录详情。
     *
     * <p>详情里会把 rawData 文本尽量解析成 JSON 对象，
     * 这样前端拿到后可以直接展示，不需要再手动做 JSON.parse。
     *
     * @param id 采集记录 ID
     * @return 采集记录详情
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> collectionDetail(Long id) {
        return toCollectionDetail(getCollection(id));
    }

    /**
     * 手工新增采集记录。
     *
     * <p>这通常用于人工补录场景。虽然是手工新增，但仍然会绑定到当前任务最近一次执行记录，
     * 这样后面的解析、加工环节还能沿着同一条 executionId 继续串起来。
     *
     * @param request 手工新增采集数据请求
     * @return 新建后的采集记录摘要
     */
    @Override
    @Transactional
    public Map<String, Object> createCollection(DataCollectionCreateRequest request) {
        validateCollectionStatus(request.getStatus());
        RpaTask task = getTask(request.getTaskId());
        RpaExecutionRecord execution = rpaExecutionRecordRepository.findTopByTaskIdOrderByStartTimeDesc(task.getId())
                .orElseThrow(() -> new BusinessException("当前任务暂无可绑定的执行记录，请先执行任务"));
        if (dataCollectionRepository.existsByExecutionId(execution.getId())) {
            throw new BusinessException("该任务最近一次执行记录已存在采集数据，请勿重复新增");
        }

        LocalDateTime now = LocalDateTime.now();
        DataCollection collection = new DataCollection();
        collection.setTaskId(task.getId());
        // 手工补录的数据也要挂到最近一次执行记录上，方便后续阶段继续按 executionId 关联。
        collection.setExecutionId(execution.getId());
        collection.setTaxpayerIdNo(request.getTaxpayerIdNo().trim());
        collection.setEnterpriseName(request.getEnterpriseName().trim());
        collection.setSourceName(request.getSourceName().trim());
        collection.setStatus(request.getStatus());
        // rawData 原样落库，同时把可选的 errorMessage 单独抽出来，便于列表和详情直接展示。
        collection.setRawData(request.getRawData().toString());
        collection.setErrorMessage(normalizeNullable(extractOptionalText(request.getRawData(), "errorMessage")));
        collection.setCollectTime(now);
        collection.setCollectionTime(now);
        dataCollectionRepository.save(collection);
        return toCreatedCollectionItem(collection, task);
    }

    /**
     * 删除采集记录。
     *
     * @param id 采集记录 ID
     * @return 被删除记录的 ID
     */
    @Override
    @Transactional
    public Map<String, Object> deleteCollection(Long id) {
        DataCollection collection = getCollection(id);
        dataCollectionRepository.delete(collection);
        return Map.of("id", id);
    }

    /*
     * ==================== 数据解析 ====================
     */

    /**
     * 解析数据分页查询。
     *
     * <p>对应“解析结果列表”页面，支持按任务、状态和解析时间区间筛选，
     * 同时返回总量、成功、处理中、失败的统计信息。
     *
     * @param query 解析数据分页查询条件
     * @return 包含 summary、分页信息和列表数据的结果
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> analysisPage(DataAnalysisPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("analysisTime"), Sort.Order.desc("id"))
        );

        Specification<DataAnalysis> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), query.getTaskId()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("analysisTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("analysisTime"), query.getEndTime()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<DataAnalysis> page = dataAnalysisRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toAnalysisPageItem)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildAnalysisSummary());
        result.put("total", page.getTotalElements());
        result.put("pageNum", page.getNumber() + 1);
        result.put("pageSize", page.getSize());
        result.put("list", list);
        return result;
    }

    /**
     * 查询解析记录详情。
     *
     * <p>详情中会把 parsedData 从 JSON 文本解析成对象，
     * 方便前端直接展示提取后的结构化结果。
     *
     * @param id 解析记录 ID
     * @return 解析记录详情
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> analysisDetail(Long id) {
        return toAnalysisDetail(getAnalysis(id));
    }

    /**
     * 删除解析记录。
     *
     * @param id 解析记录 ID
     * @return 被删除记录的 ID
     */
    @Override
    @Transactional
    public Map<String, Object> deleteAnalysis(Long id) {
        DataAnalysis analysis = getAnalysis(id);
        dataAnalysisRepository.delete(analysis);
        return Map.of("id", id);
    }

    /*
     * ==================== 数据加工 ====================
     */

    /**
     * 加工数据分页查询。
     *
     * <p>对应“加工结果列表”页面，支持按任务、状态和加工时间区间筛选，
     * 并返回当前阶段的汇总统计。
     *
     * @param query 加工数据分页查询条件
     * @return 包含 summary、分页信息和列表数据的结果
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> processingPage(DataProcessingPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("processingTime"), Sort.Order.desc("id"))
        );

        Specification<DataProcessing> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), query.getTaskId()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("processingTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("processingTime"), query.getEndTime()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<DataProcessing> page = dataProcessingRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toProcessingPageItem)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", buildProcessingSummary());
        result.put("total", page.getTotalElements());
        result.put("pageNum", page.getNumber() + 1);
        result.put("pageSize", page.getSize());
        result.put("list", list);
        return result;
    }

    /**
     * 查询加工记录详情。
     *
     * <p>这里会把 processedData、validationDetail 两段 JSON 文本尝试解析成对象，
     * 让前端既能看到加工结果，也能看到校验详情。
     *
     * @param id 加工记录 ID
     * @return 加工记录详情
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> processingDetail(Long id) {
        return toProcessingDetail(getProcessing(id));
    }

    /**
     * 删除加工记录。
     *
     * @param id 加工记录 ID
     * @return 被删除记录的 ID
     */
    @Override
    @Transactional
    public Map<String, Object> deleteProcessing(Long id) {
        DataProcessing processing = getProcessing(id);
        dataProcessingRepository.delete(processing);
        return Map.of("id", id);
    }

    /*
     * ==================== 最终业务数据 ====================
     */

    /**
     * 最终业务数据分页查询。
     *
     * <p>这个接口通常对应成品数据列表，支持按关键字、任务、税区、
     * 数据状态和创建时间区间筛选。
     *
     * @param query 业务数据分页查询条件
     * @return 业务数据分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> businessPage(BusinessDataPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("createTime"), Sort.Order.desc("id"))
        );

        Specification<BusinessDataFinal> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getKeyword())) {
                String keyword = "%" + query.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("taxpayerIdNo"), keyword),
                        cb.like(root.get("enterpriseName"), keyword)
                ));
            }
            if (query.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), query.getTaskId()));
            }
            if (StringUtils.hasText(query.getTaxAreaId())) {
                predicates.add(cb.equal(root.get("taxAreaId"), query.getTaxAreaId().trim()));
            }
            if (query.getDataStatus() != null) {
                predicates.add(cb.equal(root.get("dataStatus"), query.getDataStatus()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), query.getEndTime()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<BusinessDataFinal> page = businessDataFinalRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toBusinessPageItem)
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 查询最终业务数据详情。
     *
     * <p>详情里会把 businessData JSON 文本解析成对象，
     * 便于前端直接展示最终产出的完整业务数据。
     *
     * @param id 业务数据 ID
     * @return 业务数据详情
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> businessDetail(Long id) {
        return toBusinessDetail(getBusiness(id));
    }

    /**
     * 删除最终业务数据。
     *
     * @param id 业务数据 ID
     * @return 被删除记录的 ID
     */
    @Override
    @Transactional
    public Map<String, Object> deleteBusiness(Long id) {
        BusinessDataFinal businessData = getBusiness(id);
        businessDataFinalRepository.delete(businessData);
        return Map.of("id", id);
    }

    /*
     * ==================== 视图组装与通用辅助 ====================
     */

    /**
     * 组装采集列表项视图。
     *
     * <p>列表页只展示关键字段，所以这里不会把 rawData 整包带回，
     * 只返回任务编码、状态和基础识别信息。
     *
     * @param collection 采集实体
     * @return 采集列表项视图
     */
    private Map<String, Object> toCollectionPageItem(DataCollection collection) {
        RpaTask task = getTask(collection.getTaskId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", collection.getId());
        result.put("taskId", collection.getTaskId());
        result.put("taskCode", task.getTaskCode());
        result.put("executionId", collection.getExecutionId());
        result.put("status", collection.getStatus());
        result.put("taxpayerIdNo", collection.getTaxpayerIdNo());
        result.put("enterpriseName", collection.getEnterpriseName());
        result.put("sourceName", collection.getSourceName());
        result.put("collectionTime", collection.getCollectionTime());
        return result;
    }

    /**
     * 构建采集数据汇总统计。
     *
     * <p>供前端顶部统计卡片使用。
     *
     * @return 采集阶段汇总统计
     */
    private Map<String, Object> buildCollectionSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", dataCollectionRepository.count());
        summary.put("success", dataCollectionRepository.countByStatus(COLLECTION_STATUS_SUCCESS));
        summary.put("processing", dataCollectionRepository.countByStatus(COLLECTION_STATUS_PROCESSING));
        summary.put("failed", dataCollectionRepository.countByStatus(COLLECTION_STATUS_FAILED));
        return summary;
    }

    /**
     * 组装解析列表项视图。
     *
     * @param analysis 解析实体
     * @return 解析列表项视图
     */
    private Map<String, Object> toAnalysisPageItem(DataAnalysis analysis) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", analysis.getId());
        result.put("taskId", analysis.getTaskId());
        result.put("collectionId", analysis.getCollectionId());
        result.put("status", analysis.getStatus());
        result.put("extractedFieldCount", analysis.getExtractedFieldCount() == null ? 0 : analysis.getExtractedFieldCount());
        result.put("ruleName", normalizeEmpty(analysis.getRuleName()));
        result.put("analysisTime", analysis.getAnalysisTime());
        return result;
    }

    /**
     * 构建解析数据汇总统计。
     *
     * @return 解析阶段汇总统计
     */
    private Map<String, Object> buildAnalysisSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", dataAnalysisRepository.count());
        summary.put("success", dataAnalysisRepository.countByStatus(ANALYSIS_STATUS_SUCCESS));
        summary.put("processing", dataAnalysisRepository.countByStatus(ANALYSIS_STATUS_PROCESSING));
        summary.put("failed", dataAnalysisRepository.countByStatus(ANALYSIS_STATUS_FAILED));
        return summary;
    }

    /**
     * 组装采集详情视图，并解析原始 JSON 数据。
     *
     * @param collection 采集实体
     * @return 采集详情视图
     */
    private Map<String, Object> toCollectionDetail(DataCollection collection) {
        RpaTask task = getTask(collection.getTaskId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", collection.getId());
        result.put("taskId", collection.getTaskId());
        result.put("taskCode", task.getTaskCode());
        result.put("executionId", collection.getExecutionId());
        result.put("taxpayerIdNo", collection.getTaxpayerIdNo());
        result.put("enterpriseName", collection.getEnterpriseName());
        result.put("sourceName", collection.getSourceName());
        result.put("status", collection.getStatus());
        result.put("errorMessage", normalizeEmpty(collection.getErrorMessage()));
        result.put("collectionTime", collection.getCollectionTime());
        result.put("createTime", collection.getCreateTime());
        result.put("updateTime", collection.getUpdateTime());
        result.put("rawData", parseJsonText(collection.getRawData()));
        return result;
    }

    /**
     * 组装解析详情视图，并解析解析结果 JSON。
     *
     * @param analysis 解析实体
     * @return 解析详情视图
     */
    private Map<String, Object> toAnalysisDetail(DataAnalysis analysis) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", analysis.getId());
        result.put("taskId", analysis.getTaskId());
        result.put("collectionId", analysis.getCollectionId());
        result.put("taxpayerIdNo", analysis.getTaxpayerIdNo());
        result.put("enterpriseName", analysis.getEnterpriseName());
        result.put("status", analysis.getStatus());
        result.put("extractedFieldCount", analysis.getExtractedFieldCount() == null ? 0 : analysis.getExtractedFieldCount());
        result.put("ruleName", normalizeEmpty(analysis.getRuleName()));
        result.put("errorMessage", normalizeEmpty(analysis.getErrorMessage()));
        result.put("analysisTime", analysis.getAnalysisTime());
        result.put("createTime", analysis.getCreateTime());
        result.put("updateTime", analysis.getUpdateTime());
        result.put("parsedData", parseJsonText(analysis.getParsedData()));
        return result;
    }

    /**
     * 组装加工列表项视图。
     *
     * @param processing 加工实体
     * @return 加工列表项视图
     */
    private Map<String, Object> toProcessingPageItem(DataProcessing processing) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", processing.getId());
        result.put("taskId", processing.getTaskId());
        result.put("analysisId", processing.getAnalysisId());
        result.put("status", processing.getStatus());
        result.put("validationResult", normalizeEmpty(processing.getValidationResult()));
        result.put("processingTime", processing.getProcessingTime());
        return result;
    }

    /**
     * 构建加工数据汇总统计。
     *
     * @return 加工阶段汇总统计
     */
    private Map<String, Object> buildProcessingSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", dataProcessingRepository.count());
        summary.put("success", dataProcessingRepository.countByStatus(PROCESSING_STATUS_SUCCESS));
        summary.put("processing", dataProcessingRepository.countByStatus(PROCESSING_STATUS_PROCESSING));
        summary.put("failed", dataProcessingRepository.countByStatus(PROCESSING_STATUS_FAILED));
        return summary;
    }

    /**
     * 组装加工详情视图，并解析加工结果和校验详情 JSON。
     *
     * @param processing 加工实体
     * @return 加工详情视图
     */
    private Map<String, Object> toProcessingDetail(DataProcessing processing) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", processing.getId());
        result.put("taskId", processing.getTaskId());
        result.put("analysisId", processing.getAnalysisId());
        result.put("taxpayerIdNo", processing.getTaxpayerIdNo());
        result.put("enterpriseName", processing.getEnterpriseName());
        result.put("status", processing.getStatus());
        result.put("errorMessage", normalizeEmpty(processing.getErrorMessage()));
        result.put("validationResult", normalizeEmpty(processing.getValidationResult()));
        result.put("processTime", processing.getProcessTime());
        result.put("processingTime", processing.getProcessingTime());
        result.put("createTime", processing.getCreateTime());
        result.put("updateTime", processing.getUpdateTime());
        result.put("processedData", parseJsonText(processing.getProcessedData()));
        result.put("validationDetail", parseJsonText(processing.getValidationDetail()));
        return result;
    }

    /**
     * 组装最终业务数据列表项视图。
     *
     * @param businessData 业务数据实体
     * @return 业务数据列表项视图
     */
    private Map<String, Object> toBusinessPageItem(BusinessDataFinal businessData) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", businessData.getId());
        result.put("taskId", businessData.getTaskId());
        result.put("taxpayerIdNo", businessData.getTaxpayerIdNo());
        result.put("enterpriseName", businessData.getEnterpriseName());
        result.put("taxAreaId", normalizeDash(businessData.getTaxAreaId()));
        result.put("dataStatus", businessData.getDataStatus());
        result.put("createTime", businessData.getCreateTime());
        return result;
    }

    /**
     * 组装最终业务数据详情视图。
     *
     * @param businessData 业务数据实体
     * @return 业务数据详情视图
     */
    private Map<String, Object> toBusinessDetail(BusinessDataFinal businessData) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", businessData.getId());
        result.put("taskId", businessData.getTaskId());
        result.put("taxAreaId", normalizeDash(businessData.getTaxAreaId()));
        result.put("taxpayerIdNo", businessData.getTaxpayerIdNo());
        result.put("enterpriseName", businessData.getEnterpriseName());
        result.put("dataStatus", businessData.getDataStatus());
        result.put("createTime", businessData.getCreateTime());
        result.put("updateTime", businessData.getUpdateTime());
        result.put("businessData", parseJsonText(businessData.getBusinessData()));
        return result;
    }

    /**
     * 组装手工新增采集记录的返回结果。
     *
     * @param collection 新建后的采集实体
     * @param task 所属任务
     * @return 便于前端直接回显的新增结果
     */
    private Map<String, Object> toCreatedCollectionItem(DataCollection collection, RpaTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", collection.getId());
        result.put("taskId", collection.getTaskId());
        result.put("taskCode", task.getTaskCode());
        result.put("executionId", collection.getExecutionId());
        result.put("status", collection.getStatus());
        result.put("taxpayerIdNo", collection.getTaxpayerIdNo());
        result.put("enterpriseName", collection.getEnterpriseName());
        result.put("sourceName", collection.getSourceName());
        result.put("collectionTime", collection.getCollectionTime());
        return result;
    }

    /**
     * 按主键加载采集记录，不存在时抛出业务异常。
     *
     * @param id 采集记录 ID
     * @return 采集实体
     */
    private DataCollection getCollection(Long id) {
        return dataCollectionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("采集记录不存在"));
    }

    /**
     * 按主键加载解析记录，不存在时抛出业务异常。
     *
     * @param id 解析记录 ID
     * @return 解析实体
     */
    private DataAnalysis getAnalysis(Long id) {
        return dataAnalysisRepository.findById(id)
                .orElseThrow(() -> new BusinessException("解析记录不存在"));
    }

    /**
     * 按主键加载加工记录，不存在时抛出业务异常。
     *
     * @param id 加工记录 ID
     * @return 加工实体
     */
    private DataProcessing getProcessing(Long id) {
        return dataProcessingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("加工记录不存在"));
    }

    /**
     * 按主键加载最终业务数据，不存在时抛出业务异常。
     *
     * @param id 业务数据 ID
     * @return 业务数据实体
     */
    private BusinessDataFinal getBusiness(Long id) {
        return businessDataFinalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("业务数据不存在"));
    }

    /**
     * 按主键加载任务，不存在时抛出业务异常。
     *
     * @param id 任务 ID
     * @return 任务实体
     */
    private RpaTask getTask(Long id) {
        return rpaTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    /**
     * 校验采集状态值是否合法。
     *
     * <p>手工新增采集记录时，允许前端直接传入状态，
     * 所以这里要提前拦截非法值，避免脏数据入库。
     *
     * @param status 采集状态值
     */
    private void validateCollectionStatus(Integer status) {
        if (status == null || (status != COLLECTION_STATUS_PENDING
                && status != COLLECTION_STATUS_PROCESSING
                && status != COLLECTION_STATUS_SUCCESS
                && status != COLLECTION_STATUS_FAILED)) {
            throw new BusinessException("采集状态仅支持 0-待处理、1-采集中、2-成功、3-失败");
        }
    }

    /**
     * 从 JSON 节点中安全提取可选文本字段。
     *
     * <p>字段不存在、字段为 null 时都会直接返回 null，
     * 避免调用方每次都手动判空。
     *
     * @param jsonNode JSON 节点
     * @param fieldName 字段名
     * @return 提取出的文本；如果不存在则返回 null
     */
    private String extractOptionalText(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null || !jsonNode.has(fieldName) || jsonNode.get(fieldName).isNull()) {
            return null;
        }
        return jsonNode.get(fieldName).asText();
    }

    /**
     * 将空白字符串标准化为 null，避免无意义空串入库。
     *
     * @param value 原始字符串
     * @return 去空白后的值；如果为空则返回 null
     */
    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 将空值标准化为空字符串，方便前端直接展示。
     *
     * @param value 原始字符串
     * @return 非空则原样返回，否则返回空字符串
     */
    private String normalizeEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    /**
     * 将空值标准化为短横线占位。
     *
     * @param value 原始字符串
     * @return 非空则原样返回，否则返回短横线
     */
    private String normalizeDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    /**
     * 尝试把 JSON 文本解析为对象，解析失败时回退为原始字符串。
     *
     * <p>这样设计的好处是：如果库里存的是合法 JSON，前端能直接拿到结构化对象；
     * 如果存的不是合法 JSON，也不会报错，而是尽量把原始文本返回出去。
     *
     * @param jsonText JSON 文本
     * @return 解析后的 JSON 对象；解析失败时返回原始字符串；空值时返回空对象
     */
    private Object parseJsonText(String jsonText) {
        if (!StringUtils.hasText(jsonText)) {
            return Map.of();
        }
        try {
            return JsonUtils.toJsonNode(objectMapper, jsonText);
        } catch (Exception ignored) {
            return jsonText;
        }
    }

    /**
     * 兜底分页页码，确保最小值为 1。
     *
     * @param pageNum 原始页码
     * @return 合法页码
     */
    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    /**
     * 兜底分页大小，并限制单页最大查询量。
     *
     * @param pageSize 原始分页大小
     * @return 合法分页大小
     */
    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
    }
}
