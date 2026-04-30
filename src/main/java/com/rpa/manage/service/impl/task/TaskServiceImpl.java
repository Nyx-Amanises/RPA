package com.rpa.manage.service.impl.task;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.common.exception.BusinessException;
import com.rpa.manage.common.util.CodeGenerator;
import com.rpa.manage.common.util.SecurityUtils;
import com.rpa.manage.domain.dto.task.ExecutionPageQuery;
import com.rpa.manage.domain.dto.task.TaskPageQuery;
import com.rpa.manage.domain.dto.task.TaskUpsertRequest;
import com.rpa.manage.domain.entity.RpaExecutionRecord;
import com.rpa.manage.domain.entity.RpaProcess;
import com.rpa.manage.domain.entity.RpaProcessVersion;
import com.rpa.manage.domain.entity.RpaProcessVersionStep;
import com.rpa.manage.domain.entity.RpaRobot;
import com.rpa.manage.domain.entity.RpaTask;
import com.rpa.manage.domain.entity.BusinessDataFinal;
import com.rpa.manage.domain.entity.DataAnalysis;
import com.rpa.manage.domain.entity.DataCollection;
import com.rpa.manage.domain.entity.DataProcessing;
import com.rpa.manage.domain.repository.BusinessDataFinalRepository;
import com.rpa.manage.domain.repository.DataAnalysisRepository;
import com.rpa.manage.domain.repository.DataCollectionRepository;
import com.rpa.manage.domain.repository.RpaExecutionRecordRepository;
import com.rpa.manage.domain.repository.RpaProcessRepository;
import com.rpa.manage.domain.repository.RpaProcessVersionRepository;
import com.rpa.manage.domain.repository.RpaProcessVersionStepRepository;
import com.rpa.manage.domain.repository.RpaRobotRepository;
import com.rpa.manage.domain.repository.RpaTaskRepository;
import com.rpa.manage.domain.repository.DataProcessingRepository;
import com.rpa.manage.service.execution.ExecutionLogStreamService;
import com.rpa.manage.service.task.TaskService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 任务管理服务实现类。
 *
 * <p>这个类是项目里最核心、也最复杂的业务类之一，主要负责三大块事情：
 * 1. 任务本身的增删改查；
 * 2. 任务执行时，流程步骤脚本的串行调度；
 * 3. 执行过程中，对执行记录、机器人状态、数据采集/解析/加工/业务数据状态的联动维护。
 *
 * <p>如果你是第一次读这份代码，建议按下面顺序看：
 * 1. 先看 `page/create/detail/update/execute` 这些公开方法；
 * 2. 再看 `persistExecutionXxx`、`executeStep` 这些执行链路方法；
 * 3. 最后再看 `markXxxSuccess/Failed` 和 `toXxxView` 这些辅助方法。
 *
 * <p>这样会更容易理解“点一次执行按钮后，后端到底做了哪些事情”。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    /**
     * 任务状态常量：待执行。
     */
    private static final int TASK_STATUS_PENDING = 0;
    /**
     * 任务状态常量：执行中。
     */
    private static final int TASK_STATUS_RUNNING = 1;
    /**
     * 任务状态常量：执行成功。
     */
    private static final int TASK_STATUS_SUCCESS = 2;
    /**
     * 任务状态常量：执行失败。
     */
    private static final int TASK_STATUS_FAILED = 3;
    /**
     * 任务状态常量：排队中，等待后台线程池调度执行。
     */
    private static final int TASK_STATUS_QUEUED = 4;

    private static final int VERSION_STATUS_PUBLISHED = 1;
    private static final String FAILURE_STRATEGY_STOP = "STOP";
    private static final String FAILURE_STRATEGY_CONTINUE = "CONTINUE";
    private static final String FAILURE_STRATEGY_RETRY = "RETRY";
    private static final int DEFAULT_STEP_TIMEOUT_SECONDS = 60;

    /**
     * 机器人状态常量：离线。
     */
    private static final int ROBOT_STATUS_OFFLINE = 0;
    /**
     * 机器人状态常量：在线空闲。
     */
    private static final int ROBOT_STATUS_ONLINE = 1;
    /**
     * 机器人状态常量：工作中。
     */
    private static final int ROBOT_STATUS_WORKING = 2;

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
     * 最终业务数据状态：不可用。
     */
    private static final int BUSINESS_DATA_STATUS_UNAVAILABLE = 0;
    /**
     * 最终业务数据状态：可用。
     */
    private static final int BUSINESS_DATA_STATUS_AVAILABLE = 1;

    /*
     * 下面这些 Repository 负责访问任务、流程、机器人、执行记录以及各阶段数据表。
     * TaskServiceImpl 自己不直接写 SQL，主要负责“把这些表之间的业务关系串起来”。
     */
    private final RpaTaskRepository rpaTaskRepository;
    private final RpaProcessRepository rpaProcessRepository;
    private final RpaProcessVersionRepository rpaProcessVersionRepository;
    private final RpaProcessVersionStepRepository rpaProcessVersionStepRepository;
    private final RpaRobotRepository rpaRobotRepository;
    private final RpaExecutionRecordRepository rpaExecutionRecordRepository;
    private final DataCollectionRepository dataCollectionRepository;
    private final DataAnalysisRepository dataAnalysisRepository;
    private final DataProcessingRepository dataProcessingRepository;
    private final BusinessDataFinalRepository businessDataFinalRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExecutionLogStreamService executionLogStreamService;

    @Resource(name = "rpaTaskExecutor")
    private TaskExecutor rpaTaskExecutor;

    /**
     * Playwright 是否以无头模式运行。
     */
    @Value("${app.playwright.headless:true}")
    private boolean playwrightHeadless;

    /**
     * Playwright 默认超时时间，单位毫秒。
     */
    @Value("${app.playwright.timeout-millis:30000}")
    private double playwrightTimeoutMillis;

    @Value("${app.execution.screenshot-dir:uploads/execution-screenshots}")
    private String executionScreenshotDir;

    /**
     * 分页查询任务列表。
     *
     * <p>这里返回的是任务列表页需要的简化数据，
     * 支持按关键字、状态、创建时间区间做筛选。
     *
     * @param query 分页查询条件
     * @return 任务分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> page(TaskPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("id"))
        );

        Specification<RpaTask> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getKeyword())) {
                String keyword = "%" + query.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("taskCode"), keyword),
                        cb.like(root.get("taskName"), keyword)
                ));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), query.getEndTime()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        org.springframework.data.domain.Page<RpaTask> page = rpaTaskRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toTaskPageItem)
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 新增任务。
     *
     * <p>创建任务时，会先校验：
     * 1. 绑定的流程必须存在且处于启用状态；
     * 2. 绑定的机器人必须允许被任务占用；
     * 3. 系统会自动生成唯一任务编码。
     *
     * @param request 任务新增请求
     * @return 新建后的任务详情
     */
    @Override
    @Transactional
    public Map<String, Object> create(TaskUpsertRequest request) {
        RpaProcess process = getBindableProcess(request.getProcessId());
        RpaRobot robot = getBindableRobot(request.getRobotId());

        RpaTask task = new RpaTask();
        task.setTaskCode(generateTaskCode());
        task.setStatus(TASK_STATUS_PENDING);
        task.setCreatorUserId(SecurityUtils.currentUserId());
        applyTask(task, request, process, robot);
        rpaTaskRepository.save(task);

        log.info("创建任务完成，taskId={}, taskCode={}, processId={}, robotId={}", task.getId(), task.getTaskCode(), task.getProcessId(), task.getRobotId());
        return toTaskDetail(task, process, robot, null);
    }

    /**
     * 查询任务详情。
     *
     * <p>除了任务本身信息，这里还会补上流程、机器人、
     * 最近一次执行记录，方便前端直接展示任务运行情况。
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        RpaTask task = getTask(id);
        RpaProcess process = getProcess(task.getProcessId());
        RpaRobot robot = getRobot(task.getRobotId());
        RpaExecutionRecord latestExecution = rpaExecutionRecordRepository.findTopByTaskIdOrderByStartTimeDesc(id).orElse(null);
        return toTaskDetail(task, process, robot, latestExecution);
    }

    /**
     * 修改任务基础信息。
     *
     * <p>运行中的任务不允许修改，避免执行过程中，
     * 流程绑定、机器人绑定、纳税人信息等关键数据被中途改掉。
     *
     * @param id 任务 ID
     * @param request 任务修改请求
     * @return 修改后的任务详情
     */
    @Override
    @Transactional
    public Map<String, Object> update(Long id, TaskUpsertRequest request) {
        RpaTask task = getTask(id);
        if (isTaskBusy(task)) {
            throw new BusinessException("排队中或执行中的任务不允许修改");
        }

        RpaProcess process = getBindableProcess(request.getProcessId());
        RpaRobot robot = getBindableRobot(request.getRobotId());
        applyTask(task, request, process, robot);
        rpaTaskRepository.save(task);

        log.info("修改任务完成，taskId={}, taskCode={}, processId={}, robotId={}", task.getId(), task.getTaskCode(), task.getProcessId(), task.getRobotId());
        RpaExecutionRecord latestExecution = rpaExecutionRecordRepository.findTopByTaskIdOrderByStartTimeDesc(id).orElse(null);
        return toTaskDetail(task, process, robot, latestExecution);
    }

    /**
     * 执行任务。
     *
     * <p>这是整份代码里最关键的方法之一。整体流程可以理解为：
     * 1. 校验任务、流程、机器人当前是否允许执行；
     * 2. 创建执行记录，并把任务/机器人切到“运行中”；
     * 3. 按流程步骤顺序逐个执行脚本；
     * 4. 每步成功后收敛对应数据阶段状态；
     * 5. 全部成功则回写成功状态，任一步失败则统一回写失败状态。
     *
     * @param id 任务 ID
     * @return 执行结果摘要
     */
    @Override
    public Map<String, Object> execute(Long id) {
        ExecutionDispatch dispatch = submitExecution(id);
        try {
            rpaTaskExecutor.execute(() -> runQueuedExecution(dispatch.executionId()));
        } catch (RuntimeException ex) {
            markQueuedExecutionFailed(dispatch.executionId(), ex);
            throw new BusinessException("任务提交到后台执行线程池失败，请稍后重试");
        }

        log.info("任务已提交异步执行队列，taskId={}, taskCode={}, executionId={}",
                dispatch.taskId(), dispatch.taskCode(), dispatch.executionId());
        return Map.of(
                "taskId", dispatch.taskId(),
                "taskCode", dispatch.taskCode(),
                "executionId", dispatch.executionId(),
                "executionCode", dispatch.executionCode(),
                "status", TASK_STATUS_QUEUED,
                "message", "任务已进入异步执行队列"
        );
    }

    /**
     * 提交执行请求并占用任务/机器人资源。
     *
     * <p>这里使用数据库悲观锁保护任务和机器人状态，避免同一个任务在短时间内被重复点击触发。
     */
    private ExecutionDispatch submitExecution(Long taskId) {
        ExecutionDispatch dispatch = transactionTemplate.execute(status -> {
            RpaTask task = rpaTaskRepository.findByIdForUpdate(taskId)
                    .orElseThrow(() -> new BusinessException("任务不存在"));
            if (isTaskBusy(task)) {
                throw new BusinessException("任务已在排队或执行中，请勿重复触发");
            }

            RpaProcess process = getEnabledProcess(task.getProcessId());
            RpaRobot robot = rpaRobotRepository.findByIdForUpdate(task.getRobotId())
                    .orElseThrow(() -> new BusinessException("机器人不存在"));
            assertRobotRunnable(robot);

            RpaProcessVersion processVersion = getExecutableProcessVersion(process);
            List<RpaProcessVersionStep> steps = rpaProcessVersionStepRepository.findByProcessVersionIdOrderByStepNoAsc(processVersion.getId());
            if (steps.isEmpty()) {
                throw new BusinessException("流程发布版本尚未配置步骤，不能执行任务");
            }

            LocalDateTime queuedTime = LocalDateTime.now();
            RpaExecutionRecord executionRecord = new RpaExecutionRecord();
            executionRecord.setExecutionCode(generateExecutionCode());
            executionRecord.setTaskId(task.getId());
            executionRecord.setProcessId(process.getId());
            executionRecord.setProcessVersionId(processVersion.getId());
            executionRecord.setProcessVersionNo(processVersion.getVersionNo());
            executionRecord.setRobotId(robot.getId());
            executionRecord.setExecuteStatus(TASK_STATUS_QUEUED);
            executionRecord.setStartTime(queuedTime);
            executionRecord.setRemark("任务已进入异步执行队列，流程版本 v" + processVersion.getVersionNo());
            rpaExecutionRecordRepository.save(executionRecord);
            emitExecutionEvent(executionRecord.getId(), "execution_queued", Map.of(
                    "executionCode", executionRecord.getExecutionCode(),
                    "taskId", task.getId(),
                    "taskCode", task.getTaskCode(),
                    "executeStatus", TASK_STATUS_QUEUED,
                    "message", "任务已进入异步执行队列"
            ));

            task.setStatus(TASK_STATUS_QUEUED);
            task.setStartTime(queuedTime);
            task.setEndTime(null);
            rpaTaskRepository.save(task);

            robot.setStatus(ROBOT_STATUS_WORKING);
            robot.setCurrentTaskId(task.getId());
            robot.setLastHeartbeatTime(queuedTime);
            rpaRobotRepository.save(robot);

            return new ExecutionDispatch(task.getId(), task.getTaskCode(), executionRecord.getId(), executionRecord.getExecutionCode());
        });
        if (dispatch == null) {
            throw new BusinessException("任务提交失败，请稍后重试");
        }
        return dispatch;
    }

    /**
     * 后台线程实际执行排队任务。
     */
    private void runQueuedExecution(Long executionId) {
        try {
            ExecutionRuntime runtime = markExecutionRunning(executionId);
            if (runtime == null) {
                return;
            }
            runExecution(runtime);
        } catch (Exception ex) {
            log.warn("异步任务启动失败，executionId={}, reason={}", executionId, ex.getMessage());
            markQueuedExecutionFailed(executionId, ex);
        }
    }

    /**
     * 将执行记录从“排队中”推进到“执行中”，并返回执行所需的实体快照。
     */
    private ExecutionRuntime markExecutionRunning(Long executionId) {
        return transactionTemplate.execute(status -> {
            RpaExecutionRecord executionRecord = rpaExecutionRecordRepository.findById(executionId)
                    .orElseThrow(() -> new BusinessException("执行记录不存在"));
            if (!Objects.equals(executionRecord.getExecuteStatus(), TASK_STATUS_QUEUED)) {
                log.info("执行记录状态已变化，跳过后台启动，executionId={}, status={}",
                        executionId, executionRecord.getExecuteStatus());
                return null;
            }

            RpaTask task = rpaTaskRepository.findByIdForUpdate(executionRecord.getTaskId())
                    .orElseThrow(() -> new BusinessException("任务不存在"));
            RpaProcess process = getEnabledProcess(executionRecord.getProcessId());
            RpaRobot robot = rpaRobotRepository.findByIdForUpdate(executionRecord.getRobotId())
                    .orElseThrow(() -> new BusinessException("机器人不存在"));

            LocalDateTime startTime = LocalDateTime.now();
            executionRecord.setExecuteStatus(TASK_STATUS_RUNNING);
            executionRecord.setStartTime(startTime);
            executionRecord.setEndTime(null);
            executionRecord.setDurationSeconds(null);
            executionRecord.setErrorMessage(null);
            executionRecord.setRemark("后台线程已开始执行");
            rpaExecutionRecordRepository.save(executionRecord);

            task.setStatus(TASK_STATUS_RUNNING);
            task.setStartTime(startTime);
            task.setEndTime(null);
            rpaTaskRepository.save(task);

            robot.setStatus(ROBOT_STATUS_WORKING);
            robot.setCurrentTaskId(task.getId());
            robot.setLastHeartbeatTime(startTime);
            rpaRobotRepository.save(robot);

            emitExecutionEvent(executionRecord.getId(), "execution_running", Map.of(
                    "executionCode", executionRecord.getExecutionCode(),
                    "taskId", task.getId(),
                    "taskCode", task.getTaskCode(),
                    "executeStatus", TASK_STATUS_RUNNING,
                    "startTime", startTime,
                    "message", "后台线程已开始执行"
            ));
            return new ExecutionRuntime(task, process, robot, executionRecord, startTime);
        });
    }

    /**
     * 执行已经被调度线程取出的任务。
     */
    private void runExecution(ExecutionRuntime executionRuntime) {
        RpaTask task = executionRuntime.task();
        RpaProcess process = executionRuntime.process();
        RpaRobot robot = executionRuntime.robot();
        RpaExecutionRecord executionRecord = executionRuntime.executionRecord();
        LocalDateTime startTime = executionRuntime.startTime();
        List<RpaProcessVersionStep> steps = rpaProcessVersionStepRepository.findByProcessVersionIdOrderByStepNoAsc(
                executionRecord.getProcessVersionId()
        );
        if (steps.isEmpty()) {
            throw new BusinessException("执行记录绑定的流程版本没有步骤快照，不能执行任务");
        }

        List<Map<String, Object>> stepLogs = new ArrayList<>();
        Map<String, Object> stepContext = new LinkedHashMap<>();
        int completedStepCount = 0;
        try (PlaywrightRuntime runtime = openPlaywrightRuntime()) {
            for (RpaProcessVersionStep step : steps) {
                try {
                    emitStepStart(executionRecord, step);
                    // 每一步脚本执行完，都会把结果写进 stepContext，供后续步骤继续使用。
                    Map<String, Object> successLog = executeStepWithFailurePolicy(step, task, process, robot, executionRecord, runtime, stepContext);
                    stepLogs.add(successLog);
                    completedStepCount++;
                    persistExecutionProgress(executionRecord.getId(), stepLogs, "步骤 " + step.getStepNo() + " 执行成功");
                    emitStepLog(executionRecord, "step_success", successLog);
                    // 步骤成功后，顺手把该阶段相关业务数据的状态推进到成功。
                    syncDataStatusesOnStepSuccess(executionRecord.getId(), stepContext, step.getStepNo());
                } catch (Exception stepEx) {
                    String screenshotUrl = captureFailureScreenshot(runtime, executionRecord.getId(), step.getStepNo());
                    Map<String, Object> failureLog = toStepFailureLog(step, stepEx, stepContext, screenshotUrl);
                    stepLogs.add(failureLog);
                    persistExecutionProgress(executionRecord.getId(), stepLogs, "步骤 " + step.getStepNo() + " 执行失败");
                    emitStepLog(executionRecord, "step_failed", failureLog);
                    if (isContinueOnFailure(step)) {
                        stepContext.put("lastError", trimMessage(stepEx.getMessage(), 500));
                        stepContext.put("step_" + step.getStepNo() + "_error", trimMessage(stepEx.getMessage(), 500));
                        log.warn("流程步骤失败但按策略继续，taskId={}, executionId={}, stepNo={}, reason={}",
                                task.getId(), executionRecord.getId(), step.getStepNo(), stepEx.getMessage());
                        continue;
                    }
                    throw stepEx;
                }
            }
            // 全部步骤成功后，再做一次兜底收敛，避免残留“处理中”状态。
            reconcileDataStatusesOnTaskSuccess(executionRecord.getId());

            LocalDateTime endTime = LocalDateTime.now();
            executionRecord.setExecuteStatus(TASK_STATUS_SUCCESS);
            executionRecord.setEndTime(endTime);
            executionRecord.setDurationSeconds((int) Duration.between(startTime, endTime).getSeconds());
            executionRecord.setExecutionLog(writeJson(stepLogs));
            persistExecutionSuccess(task, robot, executionRecord, endTime);
            emitExecutionEvent(executionRecord.getId(), "execution_success", Map.of(
                    "executionCode", executionRecord.getExecutionCode(),
                    "taskId", task.getId(),
                    "taskCode", task.getTaskCode(),
                    "executeStatus", TASK_STATUS_SUCCESS,
                    "endTime", endTime,
                    "durationSeconds", executionRecord.getDurationSeconds(),
                    "message", "任务执行成功"
            ));
            executionLogStreamService.complete(executionRecord.getId());

            log.info("执行任务成功，taskId={}, taskCode={}, executionId={}", task.getId(), task.getTaskCode(), executionRecord.getId());
        } catch (Exception ex) {
            LocalDateTime endTime = LocalDateTime.now();
            // 先按失败阶段修正数据状态，再做一次整体兜底失败收敛。
            syncDataStatusesOnTaskFailure(executionRecord.getId(), stepContext, completedStepCount, trimMessage(ex.getMessage(), 500));
            reconcileDataStatusesOnTaskFailure(executionRecord.getId(), trimMessage(ex.getMessage(), 500));
            executionRecord.setExecuteStatus(TASK_STATUS_FAILED);
            executionRecord.setEndTime(endTime);
            executionRecord.setDurationSeconds((int) Duration.between(startTime, endTime).getSeconds());
            executionRecord.setErrorMessage(trimMessage(ex.getMessage(), 500));
            List<Map<String, Object>> finalLogs = ensureFailureLog(stepLogs, ex);
            executionRecord.setScreenshotUrl(firstScreenshotUrl(finalLogs));
            executionRecord.setExecutionLog(writeJson(finalLogs));
            persistExecutionFailure(task, robot, executionRecord, endTime);
            emitExecutionEvent(executionRecord.getId(), "execution_failed", buildFailureEvent(executionRecord, ex, endTime));
            executionLogStreamService.complete(executionRecord.getId());

            log.warn("执行任务失败，taskId={}, taskCode={}, reason={}", task.getId(), task.getTaskCode(), ex.getMessage());
        }
    }

    /**
     * 后台线程池拒绝任务或调度启动失败时，把排队记录收敛成失败状态。
     */
    private void markQueuedExecutionFailed(Long executionId, Exception ex) {
        transactionTemplate.executeWithoutResult(status -> {
            RpaExecutionRecord executionRecord = rpaExecutionRecordRepository.findById(executionId).orElse(null);
            if (executionRecord == null || Objects.equals(executionRecord.getExecuteStatus(), TASK_STATUS_SUCCESS)) {
                return;
            }
            RpaTask task = rpaTaskRepository.findById(executionRecord.getTaskId()).orElse(null);
            RpaRobot robot = rpaRobotRepository.findById(executionRecord.getRobotId()).orElse(null);
            LocalDateTime endTime = LocalDateTime.now();
            executionRecord.setExecuteStatus(TASK_STATUS_FAILED);
            executionRecord.setEndTime(endTime);
            executionRecord.setDurationSeconds((int) Duration.between(executionRecord.getStartTime(), endTime).getSeconds());
            executionRecord.setErrorMessage(trimMessage(ex.getMessage(), 500));
            executionRecord.setExecutionLog(writeJson(appendFailedLog(List.of(), ex)));
            rpaExecutionRecordRepository.save(executionRecord);

            if (task != null) {
                task.setStatus(TASK_STATUS_FAILED);
                task.setEndTime(endTime);
                rpaTaskRepository.save(task);
            }
            if (robot != null) {
                robot.setStatus(ROBOT_STATUS_ONLINE);
                robot.setCurrentTaskId(null);
                robot.setLastHeartbeatTime(endTime);
                rpaRobotRepository.save(robot);
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("executionCode", executionRecord.getExecutionCode());
            payload.put("executeStatus", TASK_STATUS_FAILED);
            payload.put("errorMessage", executionRecord.getErrorMessage());
            payload.put("stackTrace", stackTraceToString(ex));
            payload.put("endTime", endTime);
            payload.put("durationSeconds", executionRecord.getDurationSeconds());
            payload.put("message", "任务调度失败");
            emitExecutionEvent(executionId, "execution_failed", payload);
        });
        executionLogStreamService.complete(executionId);
    }

    /**
     * 持久化任务开始执行时的状态变化。
     *
     * <p>这里会在同一个事务模板里同时更新三处数据：
     * 1. 新增执行记录；
     * 2. 把任务改成执行中；
     * 3. 把机器人改成工作中，并记录当前任务。
     *
     * @param task 当前任务
     * @param robot 执行任务的机器人
     * @param executionRecord 执行记录
     * @param startTime 开始执行时间
     */
    private void persistExecutionStart(RpaTask task, RpaRobot robot, RpaExecutionRecord executionRecord, LocalDateTime startTime) {
        transactionTemplate.executeWithoutResult(status -> {
            rpaExecutionRecordRepository.save(executionRecord);

            task.setStatus(TASK_STATUS_RUNNING);
            task.setStartTime(startTime);
            task.setEndTime(null);
            rpaTaskRepository.save(task);

            robot.setStatus(ROBOT_STATUS_WORKING);
            robot.setCurrentTaskId(task.getId());
            robot.setLastHeartbeatTime(startTime);
            rpaRobotRepository.save(robot);
        });
    }

    /**
     * 持久化任务执行成功后的状态变化。
     *
     * @param task 当前任务
     * @param robot 执行任务的机器人
     * @param executionRecord 执行记录
     * @param endTime 执行结束时间
     */
    private void persistExecutionSuccess(RpaTask task, RpaRobot robot, RpaExecutionRecord executionRecord, LocalDateTime endTime) {
        transactionTemplate.executeWithoutResult(status -> {
            rpaExecutionRecordRepository.save(executionRecord);

            task.setStatus(TASK_STATUS_SUCCESS);
            task.setEndTime(endTime);
            rpaTaskRepository.save(task);

            robot.setStatus(ROBOT_STATUS_ONLINE);
            robot.setCurrentTaskId(null);
            robot.setLastHeartbeatTime(endTime);
            rpaRobotRepository.save(robot);
        });
    }

    /**
     * 持久化任务执行失败后的状态变化。
     *
     * @param task 当前任务
     * @param robot 执行任务的机器人
     * @param executionRecord 执行记录
     * @param endTime 执行结束时间
     */
    private void persistExecutionFailure(RpaTask task, RpaRobot robot, RpaExecutionRecord executionRecord, LocalDateTime endTime) {
        transactionTemplate.executeWithoutResult(status -> {
            rpaExecutionRecordRepository.save(executionRecord);

            task.setStatus(TASK_STATUS_FAILED);
            task.setEndTime(endTime);
            rpaTaskRepository.save(task);

            robot.setStatus(ROBOT_STATUS_ONLINE);
            robot.setCurrentTaskId(null);
            robot.setLastHeartbeatTime(endTime);
            rpaRobotRepository.save(robot);
        });
    }

    /**
     * 执行中持续刷新 execution_log，详情页和 SSE 断线重连都能拿到最新步骤。
     */
    private void persistExecutionProgress(Long executionId, List<Map<String, Object>> stepLogs, String remark) {
        transactionTemplate.executeWithoutResult(status -> {
            RpaExecutionRecord executionRecord = rpaExecutionRecordRepository.findById(executionId).orElse(null);
            if (executionRecord == null || Objects.equals(executionRecord.getExecuteStatus(), TASK_STATUS_SUCCESS)) {
                return;
            }
            executionRecord.setExecutionLog(writeJson(stepLogs));
            executionRecord.setRemark(remark);
            rpaExecutionRecordRepository.save(executionRecord);
        });
    }

    private void emitExecutionEvent(Long executionId, String eventType, Map<String, Object> payload) {
        try {
            executionLogStreamService.emit(executionId, eventType, payload);
        } catch (Exception ex) {
            log.debug("执行日志 SSE 推送失败，executionId={}, eventType={}, reason={}",
                    executionId, eventType, ex.getMessage());
        }
    }

    private void emitStepStart(RpaExecutionRecord executionRecord, RpaProcessVersionStep step) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionCode", executionRecord.getExecutionCode());
        payload.put("executeStatus", TASK_STATUS_RUNNING);
        payload.put("stepNo", step.getStepNo());
        payload.put("stepName", step.getStepName());
        payload.put("stepType", step.getStepType());
        payload.put("scriptLang", step.getScriptLang());
        payload.put("timeoutSeconds", normalizeStepTimeout(step.getTimeoutSeconds()));
        payload.put("failureStrategy", normalizeFailureStrategy(step.getFailureStrategy()));
        payload.put("message", "步骤 " + step.getStepNo() + " 开始执行");
        emitExecutionEvent(executionRecord.getId(), "step_start", payload);
    }

    private void emitStepLog(RpaExecutionRecord executionRecord, String eventType, Map<String, Object> stepLog) {
        Map<String, Object> payload = new LinkedHashMap<>(stepLog);
        payload.put("executionCode", executionRecord.getExecutionCode());
        payload.put("executeStatus", TASK_STATUS_RUNNING);
        emitExecutionEvent(executionRecord.getId(), eventType, payload);
    }

    /**
     * 删除任务。
     *
     * <p>这里只删除任务主表记录。为了避免执行链路处于中间状态时被删除，
     * 运行中的任务会直接拒绝删除请求。
     *
     * @param id 任务 ID
     * @return 仅返回被删除任务的 ID，方便前端做列表移除
     */
    @Override
    @Transactional
    public Map<String, Object> delete(Long id) {
        RpaTask task = getTask(id);
        if (isTaskBusy(task)) {
            throw new BusinessException("排队中或执行中的任务不允许删除");
        }
        rpaTaskRepository.delete(task);
        log.info("删除任务完成，taskId={}, taskCode={}", task.getId(), task.getTaskCode());
        return Map.of("id", id);
    }

    /**
     * 分页查询执行记录。
     *
     * <p>这个接口对应“任务执行历史”列表，支持按任务、执行状态、
     * 开始时间区间筛选，返回的是列表页需要的轻量字段。
     *
     * @param query 执行记录分页查询条件
     * @return 执行记录分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> executionPage(ExecutionPageQuery query) {
        Pageable pageable = PageRequest.of(
                safePageNum(query.getPageNum()) - 1,
                safePageSize(query.getPageSize()),
                Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id"))
        );

        Specification<RpaExecutionRecord> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getTaskId() != null) {
                predicates.add(cb.equal(root.get("taskId"), query.getTaskId()));
            }
            if (query.getExecuteStatus() != null) {
                predicates.add(cb.equal(root.get("executeStatus"), query.getExecuteStatus()));
            }
            if (query.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), query.getStartTime()));
            }
            if (query.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), query.getEndTime()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        org.springframework.data.domain.Page<RpaExecutionRecord> page = rpaExecutionRecordRepository.findAll(specification, pageable);
        List<Map<String, Object>> list = page.getContent().stream()
                .map(this::toExecutionPageItem)
                .toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), list);
    }

    /**
     * 查询执行记录详情。
     *
     * <p>除了执行记录本身字段，这里还会补齐任务编码、流程编码、机器人编码，
     * 并把 executionLog JSON 反序列化成 stepLogs，方便前端逐步展示执行过程。
     *
     * @param id 执行记录 ID
     * @return 执行记录详情
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> executionDetail(Long id) {
        RpaExecutionRecord execution = getExecution(id);
        RpaTask task = getTask(execution.getTaskId());
        RpaProcess process = getProcess(execution.getProcessId());
        RpaRobot robot = getRobot(execution.getRobotId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", execution.getId());
        result.put("executionCode", execution.getExecutionCode());
        result.put("taskCode", task.getTaskCode());
        result.put("processCode", process.getProcessCode());
        result.put("processVersionNo", execution.getProcessVersionNo());
        result.put("robotCode", robot.getRobotCode());
        result.put("executeStatus", execution.getExecuteStatus());
        result.put("startTime", execution.getStartTime());
        result.put("endTime", execution.getEndTime());
        result.put("durationSeconds", execution.getDurationSeconds());
        result.put("errorMessage", execution.getErrorMessage());
        result.put("screenshotUrl", execution.getScreenshotUrl());
        result.put("logContent", execution.getExecutionLog());
        result.put("stepLogs", readStepLogs(execution.getExecutionLog()));
        result.put("createTime", execution.getCreateTime());
        result.put("updateTime", execution.getUpdateTime());
        return result;
    }

    /**
     * 将请求中的任务基础字段写回实体。
     *
     * <p>这个方法只负责“赋值”，不负责校验。
     * 调用方需要先确保流程、机器人已经校验通过，再把处理好的实体传进来。
     *
     * @param task 待写入的任务实体
     * @param request 前端提交的任务参数
     * @param process 已校验通过的流程实体
     * @param robot 已校验通过的机器人实体
     */
    private void applyTask(RpaTask task, TaskUpsertRequest request, RpaProcess process, RpaRobot robot) {
        task.setTaskName(request.getTaskName().trim());
        task.setTaxpayerIdNo(request.getTaxpayerIdNo().trim());
        task.setEnterpriseName(request.getEnterpriseName().trim());
        task.setProcessId(process.getId());
        task.setRobotId(robot.getId());
        task.setRemark(normalizeNullable(request.getRemark()));
    }

    /**
     * 生成唯一任务编码。
     *
     * <p>编码生成后会立刻查库校验是否重复，
     * 如果碰撞就继续重试，直到拿到一个当前库里不存在的编码。
     *
     * @return 唯一任务编码
     */
    private String generateTaskCode() {
        String code;
        do {
            code = CodeGenerator.next("TASK_");
        } while (rpaTaskRepository.existsByTaskCode(code));
        return code;
    }

    /**
     * 生成执行记录编码。
     *
     * <p>执行记录编码目前直接复用公共编码生成器，
     * 主要用于区分每一次执行批次。
     *
     * @return 执行记录编码
     */
    private String generateExecutionCode() {
        return CodeGenerator.next("");
    }

    /**
     * 初始化 Playwright 运行环境。
     *
     * <p>这里统一创建 Playwright、浏览器、浏览器上下文和页面对象，
     * 让整个任务执行过程复用同一套浏览器资源，避免每个步骤都重复启动浏览器。
     *
     * @return 封装好的 Playwright 运行时对象
     */
    private PlaywrightRuntime openPlaywrightRuntime() {
        try {
            Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(playwrightHeadless));
            BrowserContext context = browser.newContext();
            context.setDefaultTimeout(playwrightTimeoutMillis);
            Page page = context.newPage();
            return new PlaywrightRuntime(playwright, browser, context, page);
        } catch (Exception ex) {
            throw new BusinessException("Playwright 初始化失败，请确认已安装浏览器内核: " + trimMessage(ex.getMessage(), 300));
        }
    }

    /**
     * 执行单个流程步骤脚本。
     *
     * <p>执行时会把当前任务、流程、机器人、执行记录、状态常量、
     * Playwright 对象、JDBC 工具和 stepContext 全部注入到 Groovy 脚本里。
     * 脚本执行完成后，会把结果写回上下文，并生成一条步骤日志。
     *
     * @param step 当前流程步骤
     * @param task 当前任务
     * @param process 当前任务绑定的流程
     * @param robot 当前任务绑定的机器人
     * @param executionRecord 当前执行记录
     * @param runtime 当前执行周期共享的 Playwright 运行时
     * @param stepContext 步骤共享上下文，用于前后步骤之间传值
     * @return 当前步骤的执行日志
     * @throws ScriptException Groovy 脚本执行异常
     */
    private Map<String, Object> executeStep(
            RpaProcessVersionStep step,
            RpaTask task,
            RpaProcess process,
            RpaRobot robot,
            RpaExecutionRecord executionRecord,
            PlaywrightRuntime runtime,
            Map<String, Object> stepContext
    ) throws ScriptException {
        String lang = normalizeScriptLang(step.getScriptLang());
        if (!"groovy".equals(lang)) {
            throw new BusinessException("当前仅支持 Groovy 脚本执行");
        }

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("groovy");
        if (engine == null) {
            throw new BusinessException("未检测到 Groovy 脚本引擎，请先引入 groovy-jsr223 依赖");
        }

        LocalDateTime executeTime = LocalDateTime.now();
        long stepStartMillis = System.currentTimeMillis();
        // 这里把后续脚本可能会用到的上下文对象一次性注入进去，脚本里可以直接按变量名取值。
        Bindings bindings = engine.createBindings();
        bindings.put("taskId", task.getId());
        bindings.put("taskCode", task.getTaskCode());
        bindings.put("taskName", task.getTaskName());
        bindings.put("taxpayerIdNo", task.getTaxpayerIdNo());
        bindings.put("enterpriseName", task.getEnterpriseName());
        bindings.put("processId", process.getId());
        bindings.put("processCode", process.getProcessCode());
        bindings.put("processVersionId", executionRecord.getProcessVersionId());
        bindings.put("processVersionNo", executionRecord.getProcessVersionNo());
        bindings.put("robotId", robot.getId());
        bindings.put("robotCode", robot.getRobotCode());
        bindings.put("executionId", executionRecord.getId());
        bindings.put("executionCode", executionRecord.getExecutionCode());
        bindings.put("stepNo", step.getStepNo());
        bindings.put("stepName", step.getStepName());
        bindings.put("stepType", step.getStepType());
        bindings.put("timeoutSeconds", normalizeStepTimeout(step.getTimeoutSeconds()));
        bindings.put("failureStrategy", normalizeFailureStrategy(step.getFailureStrategy()));
        bindings.put("stepContext", stepContext);
        bindings.put("taskStatusPending", TASK_STATUS_PENDING);
        bindings.put("taskStatusRunning", TASK_STATUS_RUNNING);
        bindings.put("taskStatusSuccess", TASK_STATUS_SUCCESS);
        bindings.put("taskStatusFailed", TASK_STATUS_FAILED);
        bindings.put("taskStatusQueued", TASK_STATUS_QUEUED);
        bindings.put("robotStatusOffline", ROBOT_STATUS_OFFLINE);
        bindings.put("robotStatusOnline", ROBOT_STATUS_ONLINE);
        bindings.put("robotStatusWorking", ROBOT_STATUS_WORKING);
        bindings.put("collectionStatusPending", COLLECTION_STATUS_PENDING);
        bindings.put("collectionStatusProcessing", COLLECTION_STATUS_PROCESSING);
        bindings.put("collectionStatusSuccess", COLLECTION_STATUS_SUCCESS);
        bindings.put("collectionStatusFailed", COLLECTION_STATUS_FAILED);
        bindings.put("analysisStatusPending", ANALYSIS_STATUS_PENDING);
        bindings.put("analysisStatusProcessing", ANALYSIS_STATUS_PROCESSING);
        bindings.put("analysisStatusSuccess", ANALYSIS_STATUS_SUCCESS);
        bindings.put("analysisStatusFailed", ANALYSIS_STATUS_FAILED);
        bindings.put("processingStatusPending", PROCESSING_STATUS_PENDING);
        bindings.put("processingStatusProcessing", PROCESSING_STATUS_PROCESSING);
        bindings.put("processingStatusSuccess", PROCESSING_STATUS_SUCCESS);
        bindings.put("processingStatusFailed", PROCESSING_STATUS_FAILED);
        bindings.put("businessDataStatusUnavailable", BUSINESS_DATA_STATUS_UNAVAILABLE);
        bindings.put("businessDataStatusAvailable", BUSINESS_DATA_STATUS_AVAILABLE);
        bindings.put("objectMapper", objectMapper);
        bindings.put("jdbcTemplate", jdbcTemplate);
        bindings.put("playwright", runtime.playwright());
        bindings.put("browser", runtime.browser());
        bindings.put("context", runtime.context());
        bindings.put("page", runtime.page());

        double stepTimeoutMillis = normalizeStepTimeout(step.getTimeoutSeconds()) * 1000.0;
        runtime.context().setDefaultTimeout(stepTimeoutMillis);
        runtime.page().setDefaultTimeout(stepTimeoutMillis);
        Object result = engine.eval(step.getScriptContent(), bindings);
        long durationMillis = System.currentTimeMillis() - stepStartMillis;
        // 每一步的结果都会回写到共享上下文，后续步骤可以通过 lastResult 或 step_步骤号继续取值。
        stepContext.put("lastResult", result);
        stepContext.put("step_" + step.getStepNo(), result);

        // 生成一条结构化步骤日志，后面会整体序列化到 execution_log 字段。
        Map<String, Object> logItem = new LinkedHashMap<>();
        logItem.put("stepNo", step.getStepNo());
        logItem.put("stepName", step.getStepName());
        logItem.put("stepType", step.getStepType());
        logItem.put("scriptLang", step.getScriptLang());
        logItem.put("timeoutSeconds", normalizeStepTimeout(step.getTimeoutSeconds()));
        logItem.put("failureStrategy", normalizeFailureStrategy(step.getFailureStrategy()));
        logItem.put("status", TASK_STATUS_SUCCESS);
        logItem.put("message", result == null ? "执行成功" : String.valueOf(result));
        logItem.put("executeTime", executeTime);
        logItem.put("durationMillis", durationMillis);
        logItem.put("contextKeys", new ArrayList<>(stepContext.keySet()));
        return logItem;
    }

    private Map<String, Object> executeStepWithFailurePolicy(
            RpaProcessVersionStep step,
            RpaTask task,
            RpaProcess process,
            RpaRobot robot,
            RpaExecutionRecord executionRecord,
            PlaywrightRuntime runtime,
            Map<String, Object> stepContext
    ) throws Exception {
        int maxAttempts = isRetryOnFailure(step) ? 2 : 1;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<String, Object> logItem = executeStep(step, task, process, robot, executionRecord, runtime, stepContext);
                logItem.put("attempt", attempt);
                return logItem;
            } catch (Exception ex) {
                lastException = ex;
                if (attempt < maxAttempts) {
                    stepContext.put("lastRetryError", trimMessage(ex.getMessage(), 500));
                    log.warn("流程步骤执行失败，准备重试，taskId={}, executionId={}, stepNo={}, attempt={}, reason={}",
                            task.getId(), executionRecord.getId(), step.getStepNo(), attempt, ex.getMessage());
                }
            }
        }
        throw lastException == null ? new BusinessException("流程步骤执行失败") : lastException;
    }

    private Map<String, Object> toStepFailureLog(
            RpaProcessVersionStep step,
            Exception ex,
            Map<String, Object> stepContext,
            String screenshotUrl
    ) {
        Map<String, Object> logItem = new LinkedHashMap<>();
        logItem.put("stepNo", step.getStepNo());
        logItem.put("stepName", step.getStepName());
        logItem.put("stepType", step.getStepType());
        logItem.put("scriptLang", step.getScriptLang());
        logItem.put("timeoutSeconds", normalizeStepTimeout(step.getTimeoutSeconds()));
        logItem.put("failureStrategy", normalizeFailureStrategy(step.getFailureStrategy()));
        logItem.put("status", TASK_STATUS_FAILED);
        logItem.put("message", trimMessage(ex.getMessage(), 500));
        logItem.put("stackTrace", stackTraceToString(ex));
        logItem.put("screenshotUrl", screenshotUrl);
        logItem.put("executeTime", LocalDateTime.now());
        logItem.put("contextKeys", new ArrayList<>(stepContext.keySet()));
        return logItem;
    }

    /**
     * 每个步骤执行成功后，按执行记录自动把对应数据阶段的状态收敛成“成功”。
     *
     * <p>当前约定是：
     * 第 1 步对应采集，第 2 步对应解析，第 3 步对应加工，第 4 步对应最终业务数据。
     * 这样脚本只要先把记录写成“处理中”，步骤成功后就会由后端统一推进状态。
     *
     * @param executionId 当前执行记录 ID
     * @param stepContext 步骤共享上下文（当前方法暂未直接使用，保留便于后续扩展）
     * @param stepNo 当前完成的步骤号
     */
    private void syncDataStatusesOnStepSuccess(Long executionId, Map<String, Object> stepContext, Integer stepNo) {
        if (stepNo == null) {
            return;
        }
        if (stepNo >= 1) {
            markCollectionSuccess(executionId);
        }
        if (stepNo >= 2) {
            markAnalysisSuccess(executionId);
        }
        if (stepNo >= 3) {
            markProcessingSuccess(executionId);
        }
        if (stepNo >= 4) {
            markBusinessDataAvailable(executionId);
        }
    }

    /**
     * 任务执行失败时，根据已经成功完成的步骤数推断当前失败阶段，
     * 并把该阶段对应的数据状态自动改成失败/不可用。
     *
     * <p>例如 completedStepCount=1，说明第 1 步已成功、第 2 步失败，
     * 那么这里就会把“解析阶段”相关数据统一收敛成失败状态。
     *
     * @param executionId 当前执行记录 ID
     * @param stepContext 步骤共享上下文（当前方法暂未直接使用，保留便于后续扩展）
     * @param completedStepCount 已成功完成的步骤数量
     * @param errorMessage 失败原因摘要
     */
    private void syncDataStatusesOnTaskFailure(Long executionId, Map<String, Object> stepContext, int completedStepCount, String errorMessage) {
        int failedStage = completedStepCount + 1;
        if (failedStage <= 1) {
            markCollectionFailed(executionId, errorMessage);
            return;
        }
        if (failedStage == 2) {
            markAnalysisFailed(executionId, errorMessage);
            return;
        }
        if (failedStage == 3) {
            markProcessingFailed(executionId, errorMessage);
            return;
        }
        markBusinessDataUnavailable(executionId);
    }

    /**
     * 任务整体成功后，再按 executionId 对所有阶段做一次兜底收敛。
     *
     * <p>这样可以避免脚本只写入“处理中”，或者步骤号配置异常时，
     * 某些数据状态长期停留在处理中而没有被推进到最终状态。
     *
     * @param executionId 当前执行记录 ID
     */
    private void reconcileDataStatusesOnTaskSuccess(Long executionId) {
        markCollectionSuccess(executionId, true);
        markAnalysisSuccess(executionId, true);
        markProcessingSuccess(executionId, true);
        markBusinessDataAvailable(executionId);
    }

    /**
     * 任务失败时，把当前 executionId 下仍未收敛的记录统一改成失败。
     *
     * <p>这里的原则是“尽量补失败，但不覆盖已经成功的数据”，
     * 这样既能让失败阶段有明确结果，也不会把前面已经完成的阶段误改掉。
     *
     * @param executionId 当前执行记录 ID
     * @param errorMessage 失败原因摘要
     */
    private void reconcileDataStatusesOnTaskFailure(Long executionId, String errorMessage) {
        markCollectionFailed(executionId, errorMessage);
        markAnalysisFailed(executionId, errorMessage);
        markProcessingFailed(executionId, errorMessage);
        markBusinessDataUnavailable(executionId);
    }

    /**
     * 将执行记录下的采集记录从处理中推进为成功。
     */
    private void markCollectionSuccess(Long executionId) {
        markCollectionSuccess(executionId, false);
    }

    /**
     * 将执行记录下的采集记录收敛为成功状态。
     * 可选是否把空状态记录也一并修正。
     */
    private void markCollectionSuccess(Long executionId, boolean includeNullStatus) {
        List<DataCollection> collections = dataCollectionRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (DataCollection collection : collections) {
            if (shouldMarkSuccess(collection.getStatus(), COLLECTION_STATUS_PROCESSING, includeNullStatus)) {
                collection.setStatus(COLLECTION_STATUS_SUCCESS);
                collection.setCollectionTime(LocalDateTime.now());
                changed = true;
            }
        }
        if (changed) {
            dataCollectionRepository.saveAll(collections);
        }
    }

    /**
     * 将执行记录下尚未完成的采集记录收敛为失败状态。
     */
    private void markCollectionFailed(Long executionId, String errorMessage) {
        List<DataCollection> collections = dataCollectionRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (DataCollection collection : collections) {
            if (collection.getStatus() == null
                    || collection.getStatus() == COLLECTION_STATUS_PENDING
                    || collection.getStatus() == COLLECTION_STATUS_PROCESSING) {
                collection.setStatus(COLLECTION_STATUS_FAILED);
                if (!StringUtils.hasText(collection.getErrorMessage())) {
                    collection.setErrorMessage(errorMessage);
                }
                collection.setCollectionTime(LocalDateTime.now());
                changed = true;
            }
        }
        if (changed) {
            dataCollectionRepository.saveAll(collections);
        }
    }

    /**
     * 将执行记录下的解析记录从处理中推进为成功。
     */
    private void markAnalysisSuccess(Long executionId) {
        markAnalysisSuccess(executionId, false);
    }

    /**
     * 将执行记录下的解析记录收敛为成功状态。
     */
    private void markAnalysisSuccess(Long executionId, boolean includeNullStatus) {
        List<DataAnalysis> analyses = dataAnalysisRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (DataAnalysis analysis : analyses) {
            if (shouldMarkSuccess(analysis.getStatus(), ANALYSIS_STATUS_PROCESSING, includeNullStatus)) {
                analysis.setStatus(ANALYSIS_STATUS_SUCCESS);
                analysis.setAnalysisTime(LocalDateTime.now());
                changed = true;
            }
        }
        if (changed) {
            dataAnalysisRepository.saveAll(analyses);
        }
    }

    /**
     * 将执行记录下尚未完成的解析记录收敛为失败状态。
     */
    private void markAnalysisFailed(Long executionId, String errorMessage) {
        List<DataAnalysis> analyses = dataAnalysisRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (DataAnalysis analysis : analyses) {
            if (analysis.getStatus() == null
                    || analysis.getStatus() == ANALYSIS_STATUS_PENDING
                    || analysis.getStatus() == ANALYSIS_STATUS_PROCESSING) {
                analysis.setStatus(ANALYSIS_STATUS_FAILED);
                if (!StringUtils.hasText(analysis.getErrorMessage())) {
                    analysis.setErrorMessage(errorMessage);
                }
                analysis.setAnalysisTime(LocalDateTime.now());
                changed = true;
            }
        }
        if (changed) {
            dataAnalysisRepository.saveAll(analyses);
        }
    }

    /**
     * 将执行记录下的加工记录从处理中推进为成功。
     */
    private void markProcessingSuccess(Long executionId) {
        markProcessingSuccess(executionId, false);
    }

    /**
     * 将执行记录下的加工记录收敛为成功状态。
     */
    private void markProcessingSuccess(Long executionId, boolean includeNullStatus) {
        List<DataProcessing> processings = dataProcessingRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (DataProcessing processing : processings) {
            if (shouldMarkSuccess(processing.getStatus(), PROCESSING_STATUS_PROCESSING, includeNullStatus)) {
                processing.setStatus(PROCESSING_STATUS_SUCCESS);
                processing.setProcessingTime(LocalDateTime.now());
                changed = true;
            }
        }
        if (changed) {
            dataProcessingRepository.saveAll(processings);
        }
    }

    /**
     * 将执行记录下尚未完成的加工记录收敛为失败状态。
     */
    private void markProcessingFailed(Long executionId, String errorMessage) {
        List<DataProcessing> processings = dataProcessingRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (DataProcessing processing : processings) {
            if (processing.getStatus() == null
                    || processing.getStatus() == PROCESSING_STATUS_PENDING
                    || processing.getStatus() == PROCESSING_STATUS_PROCESSING) {
                processing.setStatus(PROCESSING_STATUS_FAILED);
                if (!StringUtils.hasText(processing.getErrorMessage())) {
                    processing.setErrorMessage(errorMessage);
                }
                processing.setProcessingTime(LocalDateTime.now());
                changed = true;
            }
        }
        if (changed) {
            dataProcessingRepository.saveAll(processings);
        }
    }

    /**
     * 将执行记录下的最终业务数据标记为可用。
     */
    private void markBusinessDataAvailable(Long executionId) {
        List<BusinessDataFinal> businessDataList = businessDataFinalRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (BusinessDataFinal businessData : businessDataList) {
            if (businessData.getDataStatus() == null || businessData.getDataStatus() == BUSINESS_DATA_STATUS_UNAVAILABLE) {
                businessData.setDataStatus(BUSINESS_DATA_STATUS_AVAILABLE);
                changed = true;
            }
        }
        if (changed) {
            businessDataFinalRepository.saveAll(businessDataList);
        }
    }

    /**
     * 判断某条阶段数据是否应该从处理中推进为成功。
     */
    private boolean shouldMarkSuccess(Integer currentStatus, int processingStatus, boolean includeNullStatus) {
        if (currentStatus == null) {
            return includeNullStatus;
        }
        return currentStatus == processingStatus;
    }

    /**
     * 将执行记录下的最终业务数据标记为不可用。
     */
    private void markBusinessDataUnavailable(Long executionId) {
        List<BusinessDataFinal> businessDataList = businessDataFinalRepository.findByExecutionIdOrderByIdAsc(executionId);
        boolean changed = false;
        for (BusinessDataFinal businessData : businessDataList) {
            if (businessData.getDataStatus() == null || businessData.getDataStatus() == BUSINESS_DATA_STATUS_AVAILABLE) {
                businessData.setDataStatus(BUSINESS_DATA_STATUS_UNAVAILABLE);
                changed = true;
            }
        }
        if (changed) {
            businessDataFinalRepository.saveAll(businessDataList);
        }
    }

    /**
     * 在已有步骤日志后追加失败日志项，便于前端直接展示最终失败原因。
     */
    private List<Map<String, Object>> appendFailedLog(List<Map<String, Object>> stepLogs, Exception ex) {
        List<Map<String, Object>> result = new ArrayList<>(stepLogs);
        Map<String, Object> logItem = new LinkedHashMap<>();
        logItem.put("status", TASK_STATUS_FAILED);
        logItem.put("message", trimMessage(ex.getMessage(), 500));
        logItem.put("stackTrace", stackTraceToString(ex));
        logItem.put("executeTime", LocalDateTime.now());
        result.add(logItem);
        return result;
    }

    private List<Map<String, Object>> ensureFailureLog(List<Map<String, Object>> stepLogs, Exception ex) {
        if (!stepLogs.isEmpty()) {
            Map<String, Object> lastLog = stepLogs.get(stepLogs.size() - 1);
            if (Objects.equals(lastLog.get("status"), TASK_STATUS_FAILED)) {
                return stepLogs;
            }
        }
        return appendFailedLog(stepLogs, ex);
    }

    private String captureFailureScreenshot(PlaywrightRuntime runtime, Long executionId, Integer stepNo) {
        try {
            Path dir = Paths.get(executionScreenshotDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = "execution_" + executionId + "_step_" + (stepNo == null ? "unknown" : stepNo)
                    + "_" + System.currentTimeMillis() + ".png";
            Path file = dir.resolve(filename);
            runtime.page().screenshot(new Page.ScreenshotOptions().setPath(file));
            return "/uploads/execution-screenshots/" + filename;
        } catch (Exception screenshotEx) {
            log.warn("保存 Playwright 失败截图失败，executionId={}, stepNo={}, reason={}",
                    executionId, stepNo, screenshotEx.getMessage());
            return null;
        }
    }

    private String stackTraceToString(Throwable ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return trimMessage(writer.toString(), 4000);
    }

    private Map<String, Object> buildFailureEvent(RpaExecutionRecord executionRecord, Exception ex, LocalDateTime endTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionCode", executionRecord.getExecutionCode());
        payload.put("executeStatus", TASK_STATUS_FAILED);
        payload.put("errorMessage", executionRecord.getErrorMessage());
        payload.put("stackTrace", stackTraceToString(ex));
        payload.put("screenshotUrl", executionRecord.getScreenshotUrl());
        payload.put("endTime", endTime);
        payload.put("durationSeconds", executionRecord.getDurationSeconds());
        payload.put("message", "任务执行失败");
        return payload;
    }

    private String firstScreenshotUrl(List<Map<String, Object>> stepLogs) {
        for (Map<String, Object> stepLog : stepLogs) {
            Object screenshotUrl = stepLog.get("screenshotUrl");
            if (screenshotUrl instanceof String url && StringUtils.hasText(url)) {
                return url;
            }
        }
        return null;
    }

    /**
     * 规范化脚本语言标识，便于统一比较。
     */
    private String normalizeScriptLang(String scriptLang) {
        return scriptLang == null ? "" : scriptLang.trim().toLowerCase();
    }

    private Integer normalizeStepTimeout(Integer timeoutSeconds) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return DEFAULT_STEP_TIMEOUT_SECONDS;
        }
        return timeoutSeconds;
    }

    private String normalizeFailureStrategy(String failureStrategy) {
        if (!StringUtils.hasText(failureStrategy)) {
            return FAILURE_STRATEGY_STOP;
        }
        String normalized = failureStrategy.trim().toUpperCase();
        if (FAILURE_STRATEGY_CONTINUE.equals(normalized)
                || FAILURE_STRATEGY_RETRY.equals(normalized)
                || FAILURE_STRATEGY_STOP.equals(normalized)) {
            return normalized;
        }
        return FAILURE_STRATEGY_STOP;
    }

    private boolean isContinueOnFailure(RpaProcessVersionStep step) {
        return FAILURE_STRATEGY_CONTINUE.equals(normalizeFailureStrategy(step.getFailureStrategy()));
    }

    private boolean isRetryOnFailure(RpaProcessVersionStep step) {
        return FAILURE_STRATEGY_RETRY.equals(normalizeFailureStrategy(step.getFailureStrategy()));
    }

    /**
     * 将对象序列化为 JSON 文本。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException("执行日志序列化失败");
        }
    }

    /**
     * 将执行日志 JSON 反序列化为步骤日志列表。
     */
    private List<Map<String, Object>> readStepLogs(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception ex) {
            return List.of(Map.of("status", TASK_STATUS_FAILED, "message", "执行日志解析失败"));
        }
    }

    /**
     * 组装任务列表项视图。
     */
    private Map<String, Object> toTaskPageItem(RpaTask task) {
        RpaProcess process = getProcess(task.getProcessId());
        RpaRobot robot = getRobot(task.getRobotId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("taskCode", task.getTaskCode());
        result.put("taskName", task.getTaskName());
        result.put("taxpayerIdNo", task.getTaxpayerIdNo());
        result.put("enterpriseName", task.getEnterpriseName());
        result.put("processId", process.getId());
        result.put("processCode", process.getProcessCode());
        result.put("processName", process.getProcessName());
        result.put("processPublishedVersionNo", process.getPublishedVersionNo());
        result.put("processPublishStatus", process.getPublishStatus());
        result.put("robotId", robot.getId());
        result.put("robotCode", robot.getRobotCode());
        result.put("robotName", robot.getRobotName());
        result.put("status", task.getStatus());
        result.put("createTime", task.getCreateTime());
        return result;
    }

    /**
     * 组装任务详情视图，并可选补充最近一次执行记录。
     */
    private Map<String, Object> toTaskDetail(RpaTask task, RpaProcess process, RpaRobot robot, RpaExecutionRecord latestExecution) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", task.getId());
        result.put("taskCode", task.getTaskCode());
        result.put("taskName", task.getTaskName());
        result.put("taxpayerIdNo", task.getTaxpayerIdNo());
        result.put("enterpriseName", task.getEnterpriseName());
        result.put("processId", process.getId());
        result.put("processCode", process.getProcessCode());
        result.put("processName", process.getProcessName());
        result.put("processPublishedVersionNo", process.getPublishedVersionNo());
        result.put("processPublishStatus", process.getPublishStatus());
        result.put("robotId", robot.getId());
        result.put("robotCode", robot.getRobotCode());
        result.put("robotName", robot.getRobotName());
        result.put("status", task.getStatus());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());
        result.put("remark", task.getRemark());
        result.put("createTime", task.getCreateTime());
        result.put("updateTime", task.getUpdateTime());
        if (latestExecution != null) {
            Map<String, Object> latest = new LinkedHashMap<>();
            latest.put("id", latestExecution.getId());
            latest.put("executionCode", latestExecution.getExecutionCode());
            latest.put("processVersionNo", latestExecution.getProcessVersionNo());
            latest.put("executeStatus", latestExecution.getExecuteStatus());
            latest.put("startTime", latestExecution.getStartTime());
            latest.put("endTime", latestExecution.getEndTime());
            latest.put("durationSeconds", latestExecution.getDurationSeconds());
            latest.put("errorMessage", latestExecution.getErrorMessage());
            latest.put("createTime", latestExecution.getCreateTime());
            latest.put("updateTime", latestExecution.getUpdateTime());
            result.put("latestExecution", latest);
        }
        return result;
    }

    /**
     * 组装执行记录列表项视图。
     */
    private Map<String, Object> toExecutionPageItem(RpaExecutionRecord execution) {
        RpaTask task = getTask(execution.getTaskId());
        RpaProcess process = getProcess(execution.getProcessId());
        RpaRobot robot = getRobot(execution.getRobotId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", execution.getId());
        result.put("executionCode", execution.getExecutionCode());
        result.put("taskCode", task.getTaskCode());
        result.put("processCode", process.getProcessCode());
        result.put("processVersionNo", execution.getProcessVersionNo());
        result.put("robotCode", robot.getRobotCode());
        result.put("executeStatus", execution.getExecuteStatus());
        result.put("startTime", execution.getStartTime());
        result.put("endTime", execution.getEndTime());
        result.put("durationSeconds", execution.getDurationSeconds());
        result.put("errorMessage", execution.getErrorMessage());
        result.put("screenshotUrl", execution.getScreenshotUrl());
        result.put("createTime", execution.getCreateTime());
        result.put("updateTime", execution.getUpdateTime());
        return result;
    }

    /**
     * 按主键加载任务，不存在时抛出业务异常。
     */
    private RpaTask getTask(Long id) {
        return rpaTaskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    /**
     * 按主键加载流程，不存在时抛出业务异常。
     */
    private RpaProcess getProcess(Long id) {
        return rpaProcessRepository.findById(id)
                .orElseThrow(() -> new BusinessException("流程不存在"));
    }

    /**
     * 加载并校验流程必须处于启用状态。
     */
    private RpaProcess getEnabledProcess(Long id) {
        RpaProcess process = getProcess(id);
        if (process.getStatus() == null || process.getStatus() != 1) {
            throw new BusinessException("只能绑定启用状态的流程");
        }
        return process;
    }

    private RpaProcess getBindableProcess(Long id) {
        RpaProcess process = getEnabledProcess(id);
        RpaProcessVersion version = getExecutableProcessVersion(process);
        if (version.getStepCount() == null || version.getStepCount() <= 0) {
            throw new BusinessException("流程发布版本尚未配置步骤，不能绑定任务");
        }
        return process;
    }

    private RpaProcessVersion getExecutableProcessVersion(RpaProcess process) {
        if (process.getPublishedVersionId() == null) {
            throw new BusinessException("流程尚未发布可执行版本，请先在流程设计器中发布");
        }
        RpaProcessVersion version = rpaProcessVersionRepository.findById(process.getPublishedVersionId())
                .orElseThrow(() -> new BusinessException("流程当前发布版本不存在，请重新发布"));
        if (!Objects.equals(version.getProcessId(), process.getId())) {
            throw new BusinessException("流程发布版本归属异常，请重新发布");
        }
        if (!Objects.equals(version.getVersionStatus(), VERSION_STATUS_PUBLISHED)) {
            throw new BusinessException("流程当前发布版本已停用，请重新发布后再执行任务");
        }
        return version;
    }

    /**
     * 按主键加载机器人，不存在时抛出业务异常。
     */
    private RpaRobot getRobot(Long id) {
        return rpaRobotRepository.findById(id)
                .orElseThrow(() -> new BusinessException("机器人不存在"));
    }

    /**
     * 加载并校验机器人允许被任务绑定。
     */
    private RpaRobot getBindableRobot(Long id) {
        RpaRobot robot = getRobot(id);
        if (robot.getStatus() == null || robot.getStatus() == 0) {
            throw new BusinessException("不能绑定离线机器人");
        }
        return robot;
    }

    /**
     * 加载并校验机器人当前可执行该任务。
     * 如果机器人已被其他任务占用，会直接阻止再次执行。
     */
    private RpaRobot getRunnableRobot(Long id, Long taskId) {
        RpaRobot robot = getRobot(id);
        assertRobotRunnable(robot);
        return robot;
    }

    /**
     * 判断任务是否已经进入执行链路。
     */
    private boolean isTaskBusy(RpaTask task) {
        return task.getStatus() != null
                && (task.getStatus() == TASK_STATUS_QUEUED || task.getStatus() == TASK_STATUS_RUNNING);
    }

    /**
     * 校验机器人是否可以被本次调度占用。
     */
    private void assertRobotRunnable(RpaRobot robot) {
        if (robot.getStatus() == null || robot.getStatus() == ROBOT_STATUS_OFFLINE) {
            throw new BusinessException("机器人当前离线，不能执行任务");
        }
        if (robot.getStatus() == ROBOT_STATUS_WORKING) {
            throw new BusinessException("机器人当前正在执行其他任务");
        }
    }

    /**
     * 按主键加载执行记录，不存在时抛出业务异常。
     */
    private RpaExecutionRecord getExecution(Long id) {
        return rpaExecutionRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("执行记录不存在"));
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
     * 截断异常消息长度，避免错误信息过长导致落库失败或接口返回过大。
     */
    private String trimMessage(String message, int maxLength) {
        if (!StringUtils.hasText(message)) {
            return "执行失败";
        }
        return message.length() <= maxLength ? message : message.substring(0, maxLength);
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

    private record ExecutionDispatch(Long taskId, String taskCode, Long executionId, String executionCode) {
    }

    private record ExecutionRuntime(
            RpaTask task,
            RpaProcess process,
            RpaRobot robot,
            RpaExecutionRecord executionRecord,
            LocalDateTime startTime
    ) {
    }

    /**
     * Playwright 运行时包装对象。
     * 用于在任务执行周期内统一持有浏览器相关资源，并在结束时集中释放。
     */
    private static final class PlaywrightRuntime implements AutoCloseable {
        private final Playwright playwright;
        private final Browser browser;
        private final BrowserContext context;
        private final Page page;

        private PlaywrightRuntime(Playwright playwright, Browser browser, BrowserContext context, Page page) {
            this.playwright = playwright;
            this.browser = browser;
            this.context = context;
            this.page = page;
        }

        private Playwright playwright() {
            return playwright;
        }

        private Browser browser() {
            return browser;
        }

        private BrowserContext context() {
            return context;
        }

        private Page page() {
            return page;
        }

        /**
         * 按页面上下文、浏览器、Playwright 的顺序依次释放资源。
         */
        @Override
        public void close() {
            try {
                context.close();
            } finally {
                try {
                    browser.close();
                } finally {
                    playwright.close();
                }
            }
        }
    }
}
