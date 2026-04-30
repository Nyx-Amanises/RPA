package com.rpa.manage.service.impl.process;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.domain.dto.process.ProcessDesignRequest;
import com.rpa.manage.domain.dto.process.ProcessPageQuery;
import com.rpa.manage.domain.dto.process.ProcessUpsertRequest;
import com.rpa.manage.domain.entity.RpaProcess;
import com.rpa.manage.domain.entity.RpaProcessStep;
import com.rpa.manage.domain.entity.RpaProcessVersion;
import com.rpa.manage.domain.entity.RpaProcessVersionStep;
import com.rpa.manage.domain.repository.RpaProcessRepository;
import com.rpa.manage.domain.repository.RpaProcessStepRepository;
import com.rpa.manage.domain.repository.RpaProcessVersionRepository;
import com.rpa.manage.domain.repository.RpaProcessVersionStepRepository;
import com.rpa.manage.domain.repository.RpaTaskRepository;
import com.rpa.manage.service.process.ProcessService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 流程管理服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final RpaProcessRepository rpaProcessRepository;
    private final RpaProcessStepRepository rpaProcessStepRepository;
    private final RpaProcessVersionRepository rpaProcessVersionRepository;
    private final RpaProcessVersionStepRepository rpaProcessVersionStepRepository;
    private final RpaTaskRepository rpaTaskRepository;

    private static final int PUBLISH_STATUS_DRAFT = 0;
    private static final int PUBLISH_STATUS_PUBLISHED = 1;
    private static final int PUBLISH_STATUS_DISABLED = 2;
    private static final int VERSION_STATUS_PUBLISHED = 1;
    private static final int VERSION_STATUS_DISABLED = 2;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final String FAILURE_STRATEGY_STOP = "STOP";
    private static final String FAILURE_STRATEGY_CONTINUE = "CONTINUE";
    private static final String FAILURE_STRATEGY_RETRY = "RETRY";
    private static final Set<String> FAILURE_STRATEGIES = Set.of(
            FAILURE_STRATEGY_STOP,
            FAILURE_STRATEGY_CONTINUE,
            FAILURE_STRATEGY_RETRY
    );

    /**
     * 流程分页查询。
     * 支持按流程名称、编码和状态进行筛选。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> page(ProcessPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );

        Specification<RpaProcess> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getProcessName())) {
                predicates.add(cb.like(root.get("processName"), "%" + query.getProcessName().trim() + "%"));
            }
            if (StringUtils.hasText(query.getProcessCode())) {
                predicates.add(cb.like(root.get("processCode"), "%" + query.getProcessCode().trim() + "%"));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<RpaProcess> page = rpaProcessRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toProcessSimple)
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 新增流程基础信息。
     * 创建时只保存流程主信息，步骤设计由单独接口维护。
     */
    @Override
    @Transactional
    public Map<String, Object> create(ProcessUpsertRequest request) {
        validateProcessStatus(request.getStatus());
        String processCode = request.getProcessCode().trim();
        if (rpaProcessRepository.existsByProcessCode(processCode)) {
            throw new BusinessException("流程编码已存在");
        }

        RpaProcess process = new RpaProcess();
        process.setStepCount(0);
        process.setPublishedVersionNo(0);
        process.setPublishStatus(PUBLISH_STATUS_DRAFT);
        applyProcess(process, request);
        rpaProcessRepository.save(process);
        log.info("创建流程完成，processId={}, processCode={}, processName={}", process.getId(), process.getProcessCode(), process.getProcessName());
        return toProcessSimple(process);
    }

    /**
     * 查询流程基础详情。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        return toProcessSimple(getProcess(id));
    }

    /**
     * 查询流程设计详情。
     * 在基础信息之外额外返回按步骤顺序排列的流程步骤。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> designDetail(Long id) {
        RpaProcess process = getProcess(id);
        List<RpaProcessStep> steps = rpaProcessStepRepository.findByProcessIdOrderByStepNoAsc(id);
        return toProcessDesignDetail(process, steps);
    }

    /**
     * 修改流程基础信息。
     * 已创建的流程编码不允许变更，以免影响任务绑定关系。
     */
    @Override
    @Transactional
    public Map<String, Object> update(Long id, ProcessUpsertRequest request) {
        validateProcessStatus(request.getStatus());
        RpaProcess process = getProcess(id);
        String processCode = request.getProcessCode().trim();
        if (!process.getProcessCode().equals(processCode)) {
            throw new BusinessException("流程编码创建后不允许修改");
        }

        applyProcess(process, request);
        rpaProcessRepository.save(process);
        log.info("修改流程完成，processId={}, processCode={}, processName={}", process.getId(), process.getProcessCode(), process.getProcessName());
        return toProcessSimple(process);
    }

    /**
     * 保存流程设计步骤。
     * 会先清空旧步骤，再按最新提交结果整体重建流程步骤。
     */
    @Override
    @Transactional
    public Map<String, Object> saveDesign(Long id, ProcessDesignRequest request) {
        RpaProcess process = getProcess(id);
        validateSteps(request.getSteps());

        rpaProcessStepRepository.deleteByProcessId(id);
        // 先把旧步骤真正刷到数据库，再插入新步骤，避免同一流程下相同步骤序号触发唯一索引冲突。
        rpaProcessStepRepository.flush();
        List<RpaProcessStep> stepEntities = request.getSteps().stream()
                .map(step -> toStepEntity(id, step))
                .toList();
        rpaProcessStepRepository.saveAll(stepEntities);

        process.setStepCount(stepEntities.size());
        process.setPublishStatus(PUBLISH_STATUS_DRAFT);
        if (process.getPublishedVersionNo() == null) {
            process.setPublishedVersionNo(0);
        }
        rpaProcessRepository.save(process);
        log.info("保存流程设计完成，processId={}, processCode={}, stepCount={}", process.getId(), process.getProcessCode(), process.getStepCount());
        return toProcessDesignDetail(process, stepEntities);
    }

    @Override
    @Transactional
    public Map<String, Object> publish(Long id) {
        RpaProcess process = getProcess(id);
        List<RpaProcessStep> draftSteps = rpaProcessStepRepository.findByProcessIdOrderByStepNoAsc(id);
        validateDraftBeforePublish(draftSteps);

        List<RpaProcessVersion> activeVersions = rpaProcessVersionRepository.findByProcessIdAndVersionStatus(id, VERSION_STATUS_PUBLISHED);
        for (RpaProcessVersion activeVersion : activeVersions) {
            activeVersion.setVersionStatus(VERSION_STATUS_DISABLED);
        }
        if (!activeVersions.isEmpty()) {
            rpaProcessVersionRepository.saveAll(activeVersions);
        }

        Integer nextVersionNo = rpaProcessVersionRepository.findFirstByProcessIdOrderByVersionNoDesc(id)
                .map(RpaProcessVersion::getVersionNo)
                .map(versionNo -> versionNo + 1)
                .orElse(1);
        RpaProcessVersion version = new RpaProcessVersion();
        version.setProcessId(id);
        version.setVersionNo(nextVersionNo);
        version.setVersionStatus(VERSION_STATUS_PUBLISHED);
        version.setStepCount(draftSteps.size());
        version.setPublishTime(LocalDateTime.now());
        rpaProcessVersionRepository.save(version);

        List<RpaProcessVersionStep> versionSteps = draftSteps.stream()
                .map(step -> toVersionStepEntity(version, step))
                .toList();
        rpaProcessVersionStepRepository.saveAll(versionSteps);

        process.setStepCount(draftSteps.size());
        process.setPublishedVersionId(version.getId());
        process.setPublishedVersionNo(version.getVersionNo());
        process.setPublishStatus(PUBLISH_STATUS_PUBLISHED);
        rpaProcessRepository.save(process);
        log.info("发布流程版本完成，processId={}, processCode={}, versionNo={}, stepCount={}",
                process.getId(), process.getProcessCode(), version.getVersionNo(), versionSteps.size());
        return toProcessDesignDetail(process, draftSteps);
    }

    @Override
    @Transactional
    public Map<String, Object> disablePublishedVersion(Long id) {
        RpaProcess process = getProcess(id);
        if (process.getPublishedVersionId() == null) {
            throw new BusinessException("流程尚未发布，无需停用");
        }
        RpaProcessVersion version = rpaProcessVersionRepository.findById(process.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException("当前发布版本不存在"));
        version.setVersionStatus(VERSION_STATUS_DISABLED);
        rpaProcessVersionRepository.save(version);

        process.setPublishStatus(PUBLISH_STATUS_DISABLED);
        rpaProcessRepository.save(process);
        log.info("停用流程版本完成，processId={}, processCode={}, versionNo={}",
                process.getId(), process.getProcessCode(), version.getVersionNo());
        return toProcessSimple(process);
    }

    /**
     * 删除流程。
     * 仅允许删除尚未被任务绑定的流程。
     */
    @Override
    @Transactional
    public Map<String, Object> delete(Long id) {
        RpaProcess process = getProcess(id);
        if (rpaTaskRepository.countByProcessId(id) > 0) {
            throw new BusinessException("该流程已被任务绑定，不能删除");
        }

        rpaProcessStepRepository.deleteByProcessId(id);
        rpaProcessVersionStepRepository.deleteByProcessId(id);
        rpaProcessVersionRepository.deleteByProcessId(id);
        rpaProcessRepository.delete(process);
        log.info("删除流程完成，processId={}, processCode={}, processName={}", id, process.getProcessCode(), process.getProcessName());
        return Map.of("id", id);
    }

    /**
     * 按主键加载流程，不存在时抛出业务异常。
     */
    private RpaProcess getProcess(Long id) {
        return rpaProcessRepository.findById(id)
                .orElseThrow(() -> new BusinessException("流程不存在"));
    }

    /**
     * 将请求中的流程基础字段写回实体。
     */
    private void applyProcess(RpaProcess process, ProcessUpsertRequest request) {
        process.setProcessCode(request.getProcessCode().trim());
        process.setProcessName(request.getProcessName().trim());
        process.setDescription(normalizeNullable(request.getDescription()));
        process.setStatus(request.getStatus());
    }

    /**
     * 校验流程状态只允许为启用或禁用。
     */
    private void validateProcessStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("流程状态仅允许设置为0-禁用或1-启用");
        }
    }

    /**
     * 校验流程步骤序号必须为正数且不能重复。
     */
    private void validateSteps(List<ProcessDesignRequest.StepItem> steps) {
        Set<Integer> stepNos = new HashSet<>();
        for (ProcessDesignRequest.StepItem step : steps) {
            if (step.getStepNo() == null || step.getStepNo() <= 0) {
                throw new BusinessException("流程步骤序号必须大于0");
            }
            if (!stepNos.add(step.getStepNo())) {
                throw new BusinessException("流程步骤序号不能重复");
            }
            normalizeTimeoutSeconds(step.getTimeoutSeconds());
            normalizeFailureStrategy(step.getFailureStrategy());
        }
    }

    private void validateDraftBeforePublish(List<RpaProcessStep> steps) {
        if (steps.isEmpty()) {
            throw new BusinessException("流程草稿尚未配置步骤，不能发布");
        }
        for (RpaProcessStep step : steps) {
            if (!StringUtils.hasText(step.getStepName())
                    || !StringUtils.hasText(step.getStepType())
                    || !StringUtils.hasText(step.getScriptLang())
                    || !StringUtils.hasText(step.getScriptContent())) {
                throw new BusinessException("流程草稿存在未完善的步骤，不能发布");
            }
        }
    }

    /**
     * 将步骤请求对象转换为流程步骤实体。
     */
    private RpaProcessStep toStepEntity(Long processId, ProcessDesignRequest.StepItem step) {
        RpaProcessStep entity = new RpaProcessStep();
        entity.setProcessId(processId);
        entity.setStepNo(step.getStepNo());
        entity.setStepName(step.getStepName().trim());
        entity.setStepType(step.getStepType().trim());
        entity.setScriptLang(step.getScriptLang().trim());
        entity.setScriptContent(step.getScriptContent().trim());
        entity.setTimeoutSeconds(normalizeTimeoutSeconds(step.getTimeoutSeconds()));
        entity.setFailureStrategy(normalizeFailureStrategy(step.getFailureStrategy()));
        return entity;
    }

    private RpaProcessVersionStep toVersionStepEntity(RpaProcessVersion version, RpaProcessStep step) {
        RpaProcessVersionStep entity = new RpaProcessVersionStep();
        entity.setProcessVersionId(version.getId());
        entity.setProcessId(version.getProcessId());
        entity.setVersionNo(version.getVersionNo());
        entity.setStepNo(step.getStepNo());
        entity.setStepName(step.getStepName());
        entity.setStepType(step.getStepType());
        entity.setScriptLang(step.getScriptLang());
        entity.setScriptContent(step.getScriptContent());
        entity.setTimeoutSeconds(normalizeTimeoutSeconds(step.getTimeoutSeconds()));
        entity.setFailureStrategy(normalizeFailureStrategy(step.getFailureStrategy()));
        return entity;
    }

    /**
     * 组装流程列表和基础详情通用视图。
     */
    private Map<String, Object> toProcessSimple(RpaProcess process) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", process.getId());
        result.put("processCode", process.getProcessCode());
        result.put("processName", process.getProcessName());
        result.put("description", process.getDescription());
        result.put("stepCount", process.getStepCount());
        result.put("status", process.getStatus());
        result.put("publishedVersionId", process.getPublishedVersionId());
        result.put("publishedVersionNo", process.getPublishedVersionNo() == null ? 0 : process.getPublishedVersionNo());
        result.put("publishStatus", process.getPublishStatus() == null ? PUBLISH_STATUS_DRAFT : process.getPublishStatus());
        result.put("createTime", process.getCreateTime());
        result.put("updateTime", process.getUpdateTime());
        return result;
    }

    /**
     * 在基础流程信息上补充步骤设计明细。
     */
    private Map<String, Object> toProcessDesignDetail(RpaProcess process, List<RpaProcessStep> steps) {
        Map<String, Object> result = toProcessSimple(process);
        result.put("steps", steps.stream().map(this::toStepItem).toList());
        result.put("versions", rpaProcessVersionRepository.findByProcessIdOrderByVersionNoDesc(process.getId())
                .stream()
                .map(this::toVersionItem)
                .toList());
        return result;
    }

    /**
     * 将流程步骤实体转换为接口返回结构。
     */
    private Map<String, Object> toStepItem(RpaProcessStep step) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", step.getId());
        result.put("stepNo", step.getStepNo());
        result.put("stepName", step.getStepName());
        result.put("stepType", step.getStepType());
        result.put("scriptLang", step.getScriptLang());
        result.put("scriptContent", step.getScriptContent());
        result.put("timeoutSeconds", normalizeTimeoutSeconds(step.getTimeoutSeconds()));
        result.put("failureStrategy", normalizeFailureStrategy(step.getFailureStrategy()));
        result.put("createTime", step.getCreateTime());
        result.put("updateTime", step.getUpdateTime());
        return result;
    }

    private Map<String, Object> toVersionItem(RpaProcessVersion version) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", version.getId());
        result.put("versionNo", version.getVersionNo());
        result.put("versionStatus", version.getVersionStatus());
        result.put("stepCount", version.getStepCount());
        result.put("publishTime", version.getPublishTime());
        result.put("createTime", version.getCreateTime());
        result.put("updateTime", version.getUpdateTime());
        return result;
    }

    private Integer normalizeTimeoutSeconds(Integer timeoutSeconds) {
        if (timeoutSeconds == null) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        if (timeoutSeconds < 1 || timeoutSeconds > 3600) {
            throw new BusinessException("步骤超时时间必须在1到3600秒之间");
        }
        return timeoutSeconds;
    }

    private String normalizeFailureStrategy(String failureStrategy) {
        if (!StringUtils.hasText(failureStrategy)) {
            return FAILURE_STRATEGY_STOP;
        }
        String normalized = failureStrategy.trim().toUpperCase();
        if (!FAILURE_STRATEGIES.contains(normalized)) {
            throw new BusinessException("失败策略仅支持 STOP、CONTINUE、RETRY");
        }
        return normalized;
    }

    /**
     * 将空白字符串标准化为 null，避免无意义空串入库。
     */
    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 兜底分页页码，确保最小值为 1。
     */
    private int safePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    /**
     * 兜底分页大小，并限制单页最大查询量。
     */
    private int safePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
    }
}
