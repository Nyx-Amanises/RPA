package com.rpa.manage.service.impl.indicator;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.common.util.JsonUtils;
import com.rpa.manage.common.util.LimitedFormulaEvaluator;
import com.rpa.manage.domain.dto.indicator.IndicatorPageQuery;
import com.rpa.manage.domain.dto.indicator.IndicatorUpsertRequest;
import com.rpa.manage.domain.dto.indicator.QuotaCalculateRequest;
import com.rpa.manage.domain.dto.indicator.QuotaRuleBranchRequest;
import com.rpa.manage.domain.dto.indicator.QuotaRulePageQuery;
import com.rpa.manage.domain.dto.indicator.QuotaRuleUpsertRequest;
import com.rpa.manage.domain.entity.BusinessDataFinal;
import com.rpa.manage.domain.entity.DataAnalysis;
import com.rpa.manage.domain.entity.DataProcessing;
import com.rpa.manage.domain.entity.IndicatorDefinition;
import com.rpa.manage.domain.entity.IndicatorQuotaResult;
import com.rpa.manage.domain.entity.IndicatorQuotaRule;
import com.rpa.manage.domain.entity.IndicatorQuotaRuleBranch;
import com.rpa.manage.domain.entity.IndicatorQuotaRuleIndicator;
import com.rpa.manage.domain.entity.IndicatorResult;
import com.rpa.manage.domain.entity.RpaExecutionRecord;
import com.rpa.manage.domain.entity.RpaTask;
import com.rpa.manage.domain.repository.BusinessDataFinalRepository;
import com.rpa.manage.domain.repository.DataAnalysisRepository;
import com.rpa.manage.domain.repository.DataProcessingRepository;
import com.rpa.manage.domain.repository.IndicatorDefinitionRepository;
import com.rpa.manage.domain.repository.IndicatorQuotaResultRepository;
import com.rpa.manage.domain.repository.IndicatorQuotaRuleBranchRepository;
import com.rpa.manage.domain.repository.IndicatorQuotaRuleIndicatorRepository;
import com.rpa.manage.domain.repository.IndicatorQuotaRuleRepository;
import com.rpa.manage.domain.repository.IndicatorResultRepository;
import com.rpa.manage.domain.repository.RpaExecutionRecordRepository;
import com.rpa.manage.domain.repository.RpaTaskRepository;
import com.rpa.manage.service.indicator.IndicatorService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class IndicatorServiceImpl implements IndicatorService {

    private static final int STATUS_ENABLED = 1;
    private static final int CALC_STATUS_SUCCESS = 2;
    private static final int EXECUTION_STATUS_SUCCESS = 2;
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\$\\{([\\p{L}\\p{N}_]+)}");

    private final IndicatorDefinitionRepository indicatorDefinitionRepository;
    private final IndicatorResultRepository indicatorResultRepository;
    private final IndicatorQuotaRuleRepository quotaRuleRepository;
    private final IndicatorQuotaRuleIndicatorRepository quotaRuleIndicatorRepository;
    private final IndicatorQuotaRuleBranchRepository quotaRuleBranchRepository;
    private final IndicatorQuotaResultRepository quotaResultRepository;
    private final RpaTaskRepository rpaTaskRepository;
    private final RpaExecutionRecordRepository executionRecordRepository;
    private final DataAnalysisRepository dataAnalysisRepository;
    private final DataProcessingRepository dataProcessingRepository;
    private final BusinessDataFinalRepository businessDataFinalRepository;
    private final LimitedFormulaEvaluator formulaEvaluator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> indicatorPage(IndicatorPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );
        Specification<IndicatorDefinition> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getKeyword())) {
                String keyword = "%" + query.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("indicatorName"), keyword),
                        cb.like(root.get("indicatorCode"), keyword)
                ));
            }
            if (query.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), query.getTaskId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<IndicatorDefinition> page = indicatorDefinitionRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream().map(this::toIndicatorItem).toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> indicatorOptions() {
        return indicatorDefinitionRepository.findAll(Sort.by(Sort.Order.asc("indicatorCode"))).stream()
                .map(indicator -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", indicator.getId());
                    item.put("indicatorName", indicator.getIndicatorName());
                    item.put("indicatorCode", indicator.getIndicatorCode());
                    item.put("resultVarName", effectiveResultVarName(indicator));
                    return item;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> indicatorDetail(Long id) {
        return toIndicatorDetail(getIndicator(id));
    }

    @Override
    @Transactional
    public Map<String, Object> createIndicator(IndicatorUpsertRequest request) {
        validateIndicatorRequest(null, request);
        IndicatorDefinition indicator = new IndicatorDefinition();
        applyIndicator(indicator, request);
        indicatorDefinitionRepository.save(indicator);
        return toIndicatorDetail(indicator);
    }

    @Override
    @Transactional
    public Map<String, Object> updateIndicator(Long id, IndicatorUpsertRequest request) {
        IndicatorDefinition indicator = getIndicator(id);
        validateIndicatorRequest(id, request);
        applyIndicator(indicator, request);
        indicatorDefinitionRepository.save(indicator);
        return toIndicatorDetail(indicator);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteIndicator(Long id) {
        IndicatorDefinition indicator = getIndicator(id);
        if (quotaRuleIndicatorRepository.existsByIndicatorId(id)) {
            throw new BusinessException("该指标已被额度计算规则引用，无法删除");
        }
        indicatorDefinitionRepository.delete(indicator);
        return Map.of("id", id);
    }

    @Override
    @Transactional
    public Map<String, Object> calculateIndicator(Long id) {
        IndicatorDefinition indicator = getIndicator(id);
        if (!Integer.valueOf(STATUS_ENABLED).equals(indicator.getStatus())) {
            throw new BusinessException("指标已停用，无法计算");
        }
        RpaTask task = getTask(indicator.getTaskId());
        RpaExecutionRecord execution = executionRecordRepository
                .findTopByTaskIdAndExecuteStatusOrderByStartTimeDesc(task.getId(), EXECUTION_STATUS_SUCCESS)
                .orElseThrow(() -> new BusinessException("当前指标绑定任务暂无成功执行记录"));

        SourceContext sourceContext = buildSourceContext(task, execution);
        LimitedFormulaEvaluator.FormulaResult formulaResult = formulaEvaluator.evaluateIndicatorFormula(
                indicator.getFormulaExpression(),
                sourceContext.context(),
                effectiveResultVarName(indicator)
        );

        IndicatorResult result = indicatorResultRepository.findByIndicatorIdAndExecutionId(indicator.getId(), execution.getId())
                .orElseGet(IndicatorResult::new);
        result.setIndicatorId(indicator.getId());
        result.setIndicatorCode(indicator.getIndicatorCode());
        result.setResultVarName(effectiveResultVarName(indicator));
        result.setTaskId(task.getId());
        result.setExecutionId(execution.getId());
        result.setTaxpayerIdNo(task.getTaxpayerIdNo());
        result.setEnterpriseName(task.getEnterpriseName());
        result.setStatus(CALC_STATUS_SUCCESS);
        result.setErrorMessage(null);
        result.setCalculateTime(LocalDateTime.now());
        result.setCalculationContext(toJson(sourceContext.context()));
        result.setSourceDataRef(toJson(sourceContext.sourceRef()));
        applyIndicatorResultValue(result, formulaResult.value());
        indicatorResultRepository.save(result);
        return toIndicatorResultItem(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> quotaRulePage(QuotaRulePageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );
        Specification<IndicatorQuotaRule> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getKeyword())) {
                predicates.add(cb.like(root.get("quotaName"), "%" + query.getKeyword().trim() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<IndicatorQuotaRule> page = quotaRuleRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream().map(this::toQuotaRuleItem).toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> quotaRuleDetail(Long id) {
        return toQuotaRuleDetail(getQuotaRule(id));
    }

    @Override
    @Transactional
    public Map<String, Object> createQuotaRule(QuotaRuleUpsertRequest request) {
        validateQuotaRequest(request);
        IndicatorQuotaRule rule = new IndicatorQuotaRule();
        applyQuotaRule(rule, request);
        quotaRuleRepository.save(rule);
        replaceQuotaRelations(rule, request);
        return toQuotaRuleDetail(rule);
    }

    @Override
    @Transactional
    public Map<String, Object> updateQuotaRule(Long id, QuotaRuleUpsertRequest request) {
        validateQuotaRequest(request);
        IndicatorQuotaRule rule = getQuotaRule(id);
        applyQuotaRule(rule, request);
        quotaRuleRepository.save(rule);
        replaceQuotaRelations(rule, request);
        return toQuotaRuleDetail(rule);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteQuotaRule(Long id) {
        IndicatorQuotaRule rule = getQuotaRule(id);
        quotaRuleRepository.delete(rule);
        return Map.of("id", id);
    }

    @Override
    @Transactional
    public Map<String, Object> calculateQuotaRule(Long id, QuotaCalculateRequest request) {
        IndicatorQuotaRule rule = getQuotaRule(id);
        if (!Integer.valueOf(STATUS_ENABLED).equals(rule.getStatus())) {
            throw new BusinessException("额度计算规则已停用，无法计算");
        }
        List<IndicatorDefinition> indicators = loadRuleIndicators(rule.getId());
        List<IndicatorQuotaRuleBranch> branches = quotaRuleBranchRepository.findByQuotaRuleIdOrderByBranchOrderAscIdAsc(rule.getId())
                .stream()
                .filter(branch -> Integer.valueOf(STATUS_ENABLED).equals(branch.getStatus()))
                .toList();
        if (branches.isEmpty()) {
            throw new BusinessException("请至少配置一个启用的额度计算分支");
        }

        List<IndicatorResult> indicatorResults = selectIndicatorResults(indicators, request == null ? new QuotaCalculateRequest() : request);
        Map<String, Object> context = buildQuotaContext(indicators, indicatorResults);
        IndicatorQuotaRuleBranch matchedBranch = matchBranch(branches, context);
        Object calculatedValue = formulaEvaluator.evaluateCalculation(matchedBranch.getCalculationExpression(), context);
        context.put(rule.getResultVarName(), calculatedValue);
        String outputData = renderTemplate(rule.getOutputTemplate(), context);
        assertJson(outputData, "输出数据模板渲染后不是合法 JSON");

        IndicatorQuotaResult result = new IndicatorQuotaResult();
        result.setQuotaRuleId(rule.getId());
        result.setBranchId(matchedBranch.getId());
        result.setExecutionId(resolveSharedExecutionId(indicatorResults));
        result.setTaxpayerIdNo(indicatorResults.get(0).getTaxpayerIdNo());
        result.setEnterpriseName(indicatorResults.get(0).getEnterpriseName());
        result.setIndicatorResultIds(toJson(indicatorResults.stream().map(IndicatorResult::getId).toList()));
        result.setIndicatorValues(toJson(context));
        result.setResultVarName(rule.getResultVarName());
        result.setOutputData(outputData);
        result.setStatus(CALC_STATUS_SUCCESS);
        result.setErrorMessage(null);
        result.setCalculateTime(LocalDateTime.now());
        applyQuotaResultValue(result, calculatedValue);
        quotaResultRepository.save(result);

        rule.setLatestResultId(result.getId());
        rule.setLatestResultText(displayValue(calculatedValue));
        quotaRuleRepository.save(rule);
        return toQuotaResultItem(result, matchedBranch);
    }

    private void validateIndicatorRequest(Long id, IndicatorUpsertRequest request) {
        String code = normalizeCode(request.getIndicatorCode());
        if (id == null && indicatorDefinitionRepository.existsByIndicatorCode(code)) {
            throw new BusinessException("指标编码已存在");
        }
        if (id != null && indicatorDefinitionRepository.existsByIndicatorCodeAndIdNot(code, id)) {
            throw new BusinessException("指标编码已存在");
        }
        getTask(request.getTaskId());
        if (!StringUtils.hasText(request.getFormulaExpression())) {
            throw new BusinessException("指标计算公式不能为空");
        }
    }

    private void validateQuotaRequest(QuotaRuleUpsertRequest request) {
        if (request.getIndicatorIds() == null || request.getIndicatorIds().isEmpty()) {
            throw new BusinessException("至少选择一个指标");
        }
        if (request.getBranches() == null || request.getBranches().isEmpty()) {
            throw new BusinessException("至少配置一个额度计算分支");
        }
        long defaultCount = request.getBranches().stream().filter(branch -> Boolean.TRUE.equals(branch.getDefaultBranch())).count();
        if (defaultCount > 1) {
            throw new BusinessException("默认分支最多只能配置一个");
        }
        for (QuotaRuleBranchRequest branch : request.getBranches()) {
            if (!Boolean.TRUE.equals(branch.getDefaultBranch()) && !StringUtils.hasText(branch.getConditionExpression())) {
                throw new BusinessException("非默认分支必须填写判断条件");
            }
        }
        assertJson(request.getOutputTemplate(), "输出数据模板必须是合法 JSON");
    }

    private void applyIndicator(IndicatorDefinition indicator, IndicatorUpsertRequest request) {
        indicator.setIndicatorName(request.getIndicatorName().trim());
        indicator.setIndicatorCode(normalizeCode(request.getIndicatorCode()));
        indicator.setResultVarName(normalizeNullable(request.getResultVarName()));
        indicator.setIndicatorLogic(request.getIndicatorLogic().trim());
        indicator.setFormulaExpression(request.getFormulaExpression().trim());
        indicator.setTaskId(request.getTaskId());
        indicator.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        indicator.setRemark(normalizeNullable(request.getRemark()));
    }

    private void applyQuotaRule(IndicatorQuotaRule rule, QuotaRuleUpsertRequest request) {
        rule.setQuotaName(request.getQuotaName().trim());
        rule.setResultVarName(request.getResultVarName().trim());
        rule.setOutputTemplate(request.getOutputTemplate().trim());
        rule.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        rule.setRemark(normalizeNullable(request.getRemark()));
    }

    private void replaceQuotaRelations(IndicatorQuotaRule rule, QuotaRuleUpsertRequest request) {
        quotaRuleIndicatorRepository.deleteByQuotaRuleId(rule.getId());
        quotaRuleBranchRepository.deleteByQuotaRuleId(rule.getId());
        quotaRuleIndicatorRepository.flush();
        quotaRuleBranchRepository.flush();

        List<IndicatorDefinition> indicators = indicatorDefinitionRepository.findByIdIn(request.getIndicatorIds());
        Map<Long, IndicatorDefinition> indicatorMap = new LinkedHashMap<>();
        indicators.forEach(indicator -> indicatorMap.put(indicator.getId(), indicator));
        List<IndicatorQuotaRuleIndicator> mappings = new ArrayList<>();
        for (int i = 0; i < request.getIndicatorIds().size(); i++) {
            Long indicatorId = request.getIndicatorIds().get(i);
            IndicatorDefinition indicator = indicatorMap.get(indicatorId);
            if (indicator == null) {
                throw new BusinessException("所选指标不存在：" + indicatorId);
            }
            IndicatorQuotaRuleIndicator mapping = new IndicatorQuotaRuleIndicator();
            mapping.setQuotaRuleId(rule.getId());
            mapping.setIndicatorId(indicator.getId());
            mapping.setIndicatorCode(indicator.getIndicatorCode());
            mapping.setSortNo(i + 1);
            mappings.add(mapping);
        }
        quotaRuleIndicatorRepository.saveAll(mappings);

        List<IndicatorQuotaRuleBranch> branches = request.getBranches().stream()
                .sorted(Comparator.comparing(QuotaRuleBranchRequest::getBranchOrder))
                .map(branchRequest -> {
                    IndicatorQuotaRuleBranch branch = new IndicatorQuotaRuleBranch();
                    branch.setQuotaRuleId(rule.getId());
                    branch.setBranchName(normalizeNullable(branchRequest.getBranchName()));
                    branch.setConditionExpression(Boolean.TRUE.equals(branchRequest.getDefaultBranch())
                            ? "else"
                            : branchRequest.getConditionExpression().trim());
                    branch.setCalculationExpression(branchRequest.getCalculationExpression().trim());
                    branch.setBranchOrder(branchRequest.getBranchOrder());
                    branch.setDefaultBranch(Boolean.TRUE.equals(branchRequest.getDefaultBranch()));
                    branch.setStatus(branchRequest.getStatus() == null ? STATUS_ENABLED : branchRequest.getStatus());
                    branch.setRemark(normalizeNullable(branchRequest.getRemark()));
                    return branch;
                })
                .toList();
        quotaRuleBranchRepository.saveAll(branches);
    }

    @SuppressWarnings("unchecked")
    private SourceContext buildSourceContext(RpaTask task, RpaExecutionRecord execution) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("taskId", task.getId());
        context.put("taskCode", task.getTaskCode());
        context.put("executionId", execution.getId());
        context.put("executionCode", execution.getExecutionCode());
        context.put("taxpayerIdNo", task.getTaxpayerIdNo());
        context.put("enterpriseName", task.getEnterpriseName());
        context.put("companyId", task.getTaxpayerIdNo());

        Map<String, Object> sourceRef = new LinkedHashMap<>();
        List<Long> analysisIds = new ArrayList<>();
        for (DataAnalysis analysis : dataAnalysisRepository.findByExecutionIdOrderByIdAsc(execution.getId())) {
            analysisIds.add(analysis.getId());
            mergeMap(context, readJsonMap(analysis.getParsedData()));
        }
        List<Long> processingIds = new ArrayList<>();
        for (DataProcessing processing : dataProcessingRepository.findByExecutionIdOrderByIdAsc(execution.getId())) {
            processingIds.add(processing.getId());
            mergeMap(context, readJsonMap(processing.getProcessedData()));
            mergeMap(context, readJsonMap(processing.getValidationDetail()));
        }
        List<Long> businessIds = new ArrayList<>();
        for (BusinessDataFinal businessData : businessDataFinalRepository.findByExecutionIdOrderByIdAsc(execution.getId())) {
            businessIds.add(businessData.getId());
            Map<String, Object> businessMap = readJsonMap(businessData.getBusinessData());
            mergeMap(context, businessMap);
            Object result = businessMap.get("result");
            if (result instanceof Map<?, ?> resultMap) {
                resultMap.forEach((key, value) -> context.put(String.valueOf(key), value));
            }
        }
        applySourceAliases(context);
        sourceRef.put("analysisIds", analysisIds);
        sourceRef.put("processingIds", processingIds);
        sourceRef.put("businessDataIds", businessIds);
        return new SourceContext(context, sourceRef);
    }

    private List<IndicatorDefinition> loadRuleIndicators(Long ruleId) {
        List<IndicatorQuotaRuleIndicator> mappings = quotaRuleIndicatorRepository.findByQuotaRuleIdOrderBySortNoAscIdAsc(ruleId);
        if (mappings.isEmpty()) {
            throw new BusinessException("额度计算规则未关联指标");
        }
        Map<Long, IndicatorDefinition> indicatorMap = new LinkedHashMap<>();
        indicatorDefinitionRepository.findByIdIn(mappings.stream().map(IndicatorQuotaRuleIndicator::getIndicatorId).toList())
                .forEach(indicator -> indicatorMap.put(indicator.getId(), indicator));
        return mappings.stream()
                .map(mapping -> Optional.ofNullable(indicatorMap.get(mapping.getIndicatorId()))
                        .orElseThrow(() -> new BusinessException("额度规则关联的指标不存在：" + mapping.getIndicatorCode())))
                .toList();
    }

    private List<IndicatorResult> selectIndicatorResults(List<IndicatorDefinition> indicators, QuotaCalculateRequest request) {
        if (request.getExecutionId() != null) {
            return indicators.stream()
                    .map(indicator -> getResultByExecution(indicator, request.getExecutionId()))
                    .toList();
        }

        if (StringUtils.hasText(request.getTaxpayerIdNo()) || StringUtils.hasText(request.getEnterpriseName())) {
            String taxpayerIdNo = StringUtils.hasText(request.getTaxpayerIdNo()) ? request.getTaxpayerIdNo().trim() : null;
            String enterpriseName = StringUtils.hasText(request.getEnterpriseName()) ? request.getEnterpriseName().trim() : null;
            return indicators.stream()
                    .map(indicator -> findLatestResult(indicator, taxpayerIdNo, enterpriseName)
                            .orElseThrow(() -> new BusinessException("指标暂无同企业计算结果：" + indicator.getIndicatorCode())))
                    .toList();
        }

        List<Long> indicatorIds = indicators.stream().map(IndicatorDefinition::getId).toList();
        List<IndicatorResult> recentResults = indicatorResultRepository
                .findByIndicatorIdInAndStatusOrderByCalculateTimeDescIdDesc(indicatorIds, CALC_STATUS_SUCCESS);
        Optional<List<IndicatorResult>> sameExecutionResults = findCompleteResultGroup(indicators, recentResults, "executionId");
        if (sameExecutionResults.isPresent()) {
            return sameExecutionResults.get();
        }
        Optional<List<IndicatorResult>> sameTaxpayerResults = findCompleteResultGroup(indicators, recentResults, "taxpayerIdNo");
        if (sameTaxpayerResults.isPresent()) {
            return sameTaxpayerResults.get();
        }
        Optional<List<IndicatorResult>> sameEnterpriseResults = findCompleteResultGroup(indicators, recentResults, "enterpriseName");
        if (sameEnterpriseResults.isPresent()) {
            return sameEnterpriseResults.get();
        }

        IndicatorResult anchor = findLatestResult(indicators.get(0), request.getTaxpayerIdNo(), request.getEnterpriseName())
                .orElseThrow(() -> new BusinessException("指标暂无计算结果，请先执行指标计算：" + indicators.get(0).getIndicatorCode()));
        List<IndicatorResult> sameExecution = indicators.stream()
                .map(indicator -> indicatorResultRepository.findByIndicatorIdAndExecutionId(indicator.getId(), anchor.getExecutionId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(result -> Integer.valueOf(CALC_STATUS_SUCCESS).equals(result.getStatus()))
                .toList();
        if (sameExecution.size() == indicators.size()) {
            return sameExecution;
        }

        String taxpayerIdNo = StringUtils.hasText(request.getTaxpayerIdNo()) ? request.getTaxpayerIdNo().trim() : anchor.getTaxpayerIdNo();
        String enterpriseName = StringUtils.hasText(request.getEnterpriseName()) ? request.getEnterpriseName().trim() : anchor.getEnterpriseName();
        return indicators.stream()
                .map(indicator -> findLatestResult(indicator, taxpayerIdNo, enterpriseName)
                        .orElseThrow(() -> new BusinessException("指标暂无同企业计算结果：" + indicator.getIndicatorCode())))
                .toList();
    }

    private Optional<List<IndicatorResult>> findCompleteResultGroup(
            List<IndicatorDefinition> indicators,
            List<IndicatorResult> results,
            String groupBy
    ) {
        Map<String, Map<Long, IndicatorResult>> grouped = new LinkedHashMap<>();
        for (IndicatorResult result : results) {
            String key = resultGroupKey(result, groupBy);
            if (!StringUtils.hasText(key)) {
                continue;
            }
            grouped.computeIfAbsent(key, ignored -> new LinkedHashMap<>())
                    .putIfAbsent(result.getIndicatorId(), result);
        }
        for (Map<Long, IndicatorResult> resultMap : grouped.values()) {
            if (resultMap.size() == indicators.size()) {
                return Optional.of(indicators.stream()
                        .map(indicator -> resultMap.get(indicator.getId()))
                        .toList());
            }
        }
        return Optional.empty();
    }

    private String resultGroupKey(IndicatorResult result, String groupBy) {
        return switch (groupBy) {
            case "executionId" -> String.valueOf(result.getExecutionId());
            case "taxpayerIdNo" -> result.getTaxpayerIdNo();
            case "enterpriseName" -> result.getEnterpriseName();
            default -> "";
        };
    }

    private Optional<IndicatorResult> findLatestResult(IndicatorDefinition indicator, String taxpayerIdNo, String enterpriseName) {
        if (StringUtils.hasText(taxpayerIdNo)) {
            return indicatorResultRepository.findTopByIndicatorIdAndTaxpayerIdNoAndStatusOrderByCalculateTimeDescIdDesc(
                    indicator.getId(),
                    taxpayerIdNo.trim(),
                    CALC_STATUS_SUCCESS
            );
        }
        if (StringUtils.hasText(enterpriseName)) {
            return indicatorResultRepository.findTopByIndicatorIdAndEnterpriseNameAndStatusOrderByCalculateTimeDescIdDesc(
                    indicator.getId(),
                    enterpriseName.trim(),
                    CALC_STATUS_SUCCESS
            );
        }
        return indicatorResultRepository.findTopByIndicatorIdAndStatusOrderByCalculateTimeDescIdDesc(indicator.getId(), CALC_STATUS_SUCCESS);
    }

    private IndicatorResult getResultByExecution(IndicatorDefinition indicator, Long executionId) {
        return indicatorResultRepository.findByIndicatorIdAndExecutionId(indicator.getId(), executionId)
                .filter(result -> Integer.valueOf(CALC_STATUS_SUCCESS).equals(result.getStatus()))
                .orElseThrow(() -> new BusinessException("指定执行批次缺少指标结果：" + indicator.getIndicatorCode()));
    }

    private Map<String, Object> buildQuotaContext(List<IndicatorDefinition> indicators, List<IndicatorResult> results) {
        Map<Long, IndicatorDefinition> indicatorMap = new LinkedHashMap<>();
        indicators.forEach(indicator -> indicatorMap.put(indicator.getId(), indicator));
        Map<String, Object> context = new LinkedHashMap<>();
        IndicatorResult first = results.get(0);
        context.put("taxpayerIdNo", first.getTaxpayerIdNo());
        context.put("enterpriseName", first.getEnterpriseName());
        context.put("companyId", first.getTaxpayerIdNo());
        for (IndicatorResult result : results) {
            IndicatorDefinition indicator = indicatorMap.get(result.getIndicatorId());
            Object value = resultValue(result);
            putAlias(context, result.getIndicatorCode(), value);
            putAlias(context, toLowerCamel(result.getIndicatorCode()), value);
            putBusinessIndicatorAlias(context, result.getIndicatorCode(), value);
            if (StringUtils.hasText(result.getResultVarName())) {
                putAlias(context, result.getResultVarName(), value);
            }
            if (indicator != null && StringUtils.hasText(indicator.getResultVarName())) {
                putAlias(context, indicator.getResultVarName(), value);
            }
            if (indicator != null && StringUtils.hasText(indicator.getIndicatorName())) {
                putAlias(context, indicator.getIndicatorName(), value);
            }
        }
        return context;
    }

    private void putBusinessIndicatorAlias(Map<String, Object> context, String indicatorCode, Object value) {
        if (!StringUtils.hasText(indicatorCode)) {
            return;
        }
        switch (normalizeCode(indicatorCode)) {
            case "SALE_JSHJ_SUM" -> putAlias(context, "saleJshjSum12m", value);
            case "PURCHASE_JSHJ_SUM" -> putAlias(context, "purchaseJshjSum12m", value);
            case "ABNORMAL_INVOICE_COUNT" -> putAlias(context, "abnormalInvoiceCount12m", value);
            case "BASE_CREDIT_LIMIT" -> putAlias(context, "baseCreditLimit", value);
            case "TAX_RATE" -> putAlias(context, "taxRate", value);
            case "PROFIT_RATE" -> putAlias(context, "profitRate", value);
            case "ASSET_DEBT_RATIO" -> putAlias(context, "assetDebtRatio", value);
            case "CURRENT_RATIO" -> putAlias(context, "currentRatio", value);
            case "AR_TURNOVER" -> putAlias(context, "arTurnover", value);
            case "RISK_SCORE" -> putAlias(context, "riskScore", value);
            default -> {
            }
        }
    }

    private IndicatorQuotaRuleBranch matchBranch(List<IndicatorQuotaRuleBranch> branches, Map<String, Object> context) {
        for (IndicatorQuotaRuleBranch branch : branches) {
            if (Boolean.TRUE.equals(branch.getDefaultBranch()) || formulaEvaluator.evaluateCondition(branch.getConditionExpression(), context)) {
                return branch;
            }
        }
        throw new BusinessException("没有命中的额度计算分支，请配置默认分支");
    }

    private Long resolveSharedExecutionId(List<IndicatorResult> results) {
        Long first = results.get(0).getExecutionId();
        return results.stream().allMatch(result -> first.equals(result.getExecutionId())) ? first : null;
    }

    private Map<String, Object> toIndicatorItem(IndicatorDefinition indicator) {
        RpaTask task = getTask(indicator.getTaskId());
        Optional<IndicatorResult> latestResult = indicatorResultRepository
                .findTopByIndicatorIdAndStatusOrderByCalculateTimeDescIdDesc(indicator.getId(), CALC_STATUS_SUCCESS);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", indicator.getId());
        result.put("indicatorName", indicator.getIndicatorName());
        result.put("indicatorCode", indicator.getIndicatorCode());
        result.put("resultVarName", effectiveResultVarName(indicator));
        result.put("indicatorLogic", indicator.getIndicatorLogic());
        result.put("formulaExpression", indicator.getFormulaExpression());
        result.put("taskId", indicator.getTaskId());
        result.put("taskName", task.getTaskName());
        result.put("status", indicator.getStatus());
        result.put("latestResult", latestResult.map(this::displayResult).orElse(""));
        result.put("latestCalculateTime", latestResult.map(IndicatorResult::getCalculateTime).orElse(null));
        result.put("createTime", indicator.getCreateTime());
        return result;
    }

    private Map<String, Object> toIndicatorDetail(IndicatorDefinition indicator) {
        Map<String, Object> result = toIndicatorItem(indicator);
        result.put("remark", normalizeEmpty(indicator.getRemark()));
        result.put("updateTime", indicator.getUpdateTime());
        return result;
    }

    private Map<String, Object> toIndicatorResultItem(IndicatorResult result) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", result.getId());
        item.put("indicatorId", result.getIndicatorId());
        item.put("indicatorCode", result.getIndicatorCode());
        item.put("resultVarName", result.getResultVarName());
        item.put("taskId", result.getTaskId());
        item.put("executionId", result.getExecutionId());
        item.put("taxpayerIdNo", result.getTaxpayerIdNo());
        item.put("enterpriseName", result.getEnterpriseName());
        item.put("resultType", result.getResultType());
        item.put("resultValue", resultValue(result));
        item.put("displayValue", displayResult(result));
        item.put("calculateTime", result.getCalculateTime());
        return item;
    }

    private Map<String, Object> toQuotaRuleItem(IndicatorQuotaRule rule) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rule.getId());
        result.put("quotaName", rule.getQuotaName());
        result.put("resultVarName", rule.getResultVarName());
        result.put("indicatorCodes", quotaRuleIndicatorRepository.findByQuotaRuleIdOrderBySortNoAscIdAsc(rule.getId())
                .stream()
                .map(IndicatorQuotaRuleIndicator::getIndicatorCode)
                .toList());
        result.put("latestResult", normalizeEmpty(rule.getLatestResultText()));
        result.put("latestResultId", rule.getLatestResultId());
        result.put("outputTemplate", rule.getOutputTemplate());
        result.put("status", rule.getStatus());
        result.put("createTime", rule.getCreateTime());
        return result;
    }

    private Map<String, Object> toQuotaRuleDetail(IndicatorQuotaRule rule) {
        Map<String, Object> result = toQuotaRuleItem(rule);
        result.put("indicatorIds", quotaRuleIndicatorRepository.findByQuotaRuleIdOrderBySortNoAscIdAsc(rule.getId())
                .stream()
                .map(IndicatorQuotaRuleIndicator::getIndicatorId)
                .toList());
        result.put("branches", quotaRuleBranchRepository.findByQuotaRuleIdOrderByBranchOrderAscIdAsc(rule.getId())
                .stream()
                .map(this::toBranchItem)
                .toList());
        result.put("remark", normalizeEmpty(rule.getRemark()));
        result.put("updateTime", rule.getUpdateTime());
        return result;
    }

    private Map<String, Object> toBranchItem(IndicatorQuotaRuleBranch branch) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", branch.getId());
        item.put("branchName", normalizeEmpty(branch.getBranchName()));
        item.put("conditionExpression", normalizeEmpty(branch.getConditionExpression()));
        item.put("calculationExpression", branch.getCalculationExpression());
        item.put("branchOrder", branch.getBranchOrder());
        item.put("defaultBranch", Boolean.TRUE.equals(branch.getDefaultBranch()));
        item.put("status", branch.getStatus());
        item.put("remark", normalizeEmpty(branch.getRemark()));
        return item;
    }

    private Map<String, Object> toQuotaResultItem(IndicatorQuotaResult result, IndicatorQuotaRuleBranch branch) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", result.getId());
        item.put("quotaRuleId", result.getQuotaRuleId());
        item.put("branchId", result.getBranchId());
        item.put("branchName", branch == null ? "" : normalizeEmpty(branch.getBranchName()));
        item.put("executionId", result.getExecutionId());
        item.put("taxpayerIdNo", result.getTaxpayerIdNo());
        item.put("enterpriseName", result.getEnterpriseName());
        item.put("resultVarName", result.getResultVarName());
        item.put("resultType", result.getResultType());
        item.put("resultValue", quotaResultValue(result));
        item.put("displayValue", displayQuotaResult(result));
        item.put("outputData", readJsonObject(result.getOutputData()));
        item.put("calculateTime", result.getCalculateTime());
        return item;
    }

    private void applyIndicatorResultValue(IndicatorResult result, Object value) {
        result.setResultValueDecimal(null);
        result.setResultValueText(null);
        result.setResultValueJson(null);
        if (value instanceof BigDecimal decimal) {
            result.setResultType("NUMBER");
            result.setResultValueDecimal(decimal.setScale(6, RoundingMode.HALF_UP));
        } else if (value instanceof Number number) {
            result.setResultType("NUMBER");
            result.setResultValueDecimal(new BigDecimal(number.toString()).setScale(6, RoundingMode.HALF_UP));
        } else if (value instanceof Boolean bool) {
            result.setResultType("BOOLEAN");
            result.setResultValueText(String.valueOf(bool));
        } else if (value instanceof Map<?, ?> || value instanceof List<?>) {
            result.setResultType("JSON");
            result.setResultValueJson(toJson(value));
        } else {
            result.setResultType("STRING");
            result.setResultValueText(String.valueOf(value));
        }
    }

    private void applyQuotaResultValue(IndicatorQuotaResult result, Object value) {
        result.setCalculatedValueDecimal(null);
        result.setCalculatedValueText(null);
        result.setCalculatedValueJson(null);
        if (value instanceof BigDecimal decimal) {
            result.setResultType("NUMBER");
            result.setCalculatedValueDecimal(decimal.setScale(6, RoundingMode.HALF_UP));
        } else if (value instanceof Number number) {
            result.setResultType("NUMBER");
            result.setCalculatedValueDecimal(new BigDecimal(number.toString()).setScale(6, RoundingMode.HALF_UP));
        } else if (value instanceof Boolean bool) {
            result.setResultType("BOOLEAN");
            result.setCalculatedValueText(String.valueOf(bool));
        } else if (value instanceof Map<?, ?> || value instanceof List<?>) {
            result.setResultType("JSON");
            result.setCalculatedValueJson(toJson(value));
        } else {
            result.setResultType("STRING");
            result.setCalculatedValueText(String.valueOf(value));
        }
    }

    private Object resultValue(IndicatorResult result) {
        return switch (result.getResultType()) {
            case "NUMBER" -> result.getResultValueDecimal();
            case "JSON" -> readJsonObject(result.getResultValueJson());
            default -> result.getResultValueText();
        };
    }

    private Object quotaResultValue(IndicatorQuotaResult result) {
        return switch (result.getResultType()) {
            case "NUMBER" -> result.getCalculatedValueDecimal();
            case "JSON" -> readJsonObject(result.getCalculatedValueJson());
            default -> result.getCalculatedValueText();
        };
    }

    private String displayResult(IndicatorResult result) {
        return displayValue(resultValue(result));
    }

    private String displayQuotaResult(IndicatorQuotaResult result) {
        return displayValue(quotaResultValue(result));
    }

    private String displayValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value == null ? "" : String.valueOf(value);
    }

    private String renderTemplate(String template, Map<String, Object> context) {
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context.get(key);
            if (value == null) {
                throw new BusinessException("输出模板变量不存在：" + key);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonMap(String jsonText) {
        if (!StringUtils.hasText(jsonText)) {
            return new LinkedHashMap<>();
        }
        try {
            Object value = objectMapper.readValue(jsonText, Map.class);
            return value instanceof Map<?, ?> map ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private Object readJsonObject(String jsonText) {
        if (!StringUtils.hasText(jsonText)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonText, Object.class);
        } catch (Exception ignored) {
            return jsonText;
        }
    }

    private void assertJson(String jsonText, String message) {
        try {
            objectMapper.readTree(jsonText);
        } catch (Exception ex) {
            throw new BusinessException(message);
        }
    }

    private void mergeMap(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, value) -> {
            target.put(key, value);
            if ("jshjText".equals(key)) {
                target.put("jshj", value);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void applySourceAliases(Map<String, Object> context) {
        Object invoices = context.get("invoices");
        if (invoices instanceof List<?> rows) {
            context.putIfAbsent("发票列表", rows);
            for (Object row : rows) {
                if (row instanceof Map<?, ?> rowMap) {
                    Map<String, Object> mutableRow = (Map<String, Object>) rowMap;
                    mutableRow.computeIfAbsent("jshj", ignored -> mutableRow.get("jshjText"));
                    Optional.ofNullable(toDecimalOrNull(mutableRow.get("monthDiffToAppDate")))
                            .ifPresent(diff -> mutableRow.put("近12个月",
                                    diff.compareTo(BigDecimal.ONE) >= 0 && diff.compareTo(BigDecimal.valueOf(12)) <= 0));
                }
            }
        }
    }

    private BigDecimal toDecimalOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return value instanceof BigDecimal decimal
                    ? decimal
                    : new BigDecimal(String.valueOf(value).trim().replace(",", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void putAlias(Map<String, Object> context, String alias, Object value) {
        if (StringUtils.hasText(alias)) {
            context.put(alias.trim(), value);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private String effectiveResultVarName(IndicatorDefinition indicator) {
        if (StringUtils.hasText(indicator.getResultVarName())) {
            return indicator.getResultVarName().trim();
        }
        String assignmentName = formulaEvaluator.extractAssignmentName(indicator.getFormulaExpression());
        return StringUtils.hasText(assignmentName) ? assignmentName : toLowerCamel(indicator.getIndicatorCode());
    }

    private String toLowerCamel(String value) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toLowerCase().toCharArray()) {
            if (ch == '_' || ch == '-' || ch == ' ') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String toJson(Object value) {
        return JsonUtils.toJsonString(objectMapper, value);
    }

    private IndicatorDefinition getIndicator(Long id) {
        return indicatorDefinitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("指标不存在"));
    }

    private IndicatorQuotaRule getQuotaRule(Long id) {
        return quotaRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("额度计算规则不存在"));
    }

    private RpaTask getTask(Long id) {
        return rpaTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
    }

    private record SourceContext(Map<String, Object> context, Map<String, Object> sourceRef) {
    }
}
