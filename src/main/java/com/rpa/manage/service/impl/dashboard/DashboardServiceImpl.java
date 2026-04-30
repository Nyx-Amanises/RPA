package com.rpa.manage.service.impl.dashboard;

import com.rpa.manage.domain.entity.DataCollection;
import com.rpa.manage.domain.entity.RpaProcess;
import com.rpa.manage.domain.entity.RpaRobot;
import com.rpa.manage.domain.entity.RpaTask;
import com.rpa.manage.domain.repository.BusinessDataFinalRepository;
import com.rpa.manage.domain.repository.DataCollectionRepository;
import com.rpa.manage.domain.repository.RpaExecutionRecordRepository;
import com.rpa.manage.domain.repository.RpaProcessRepository;
import com.rpa.manage.domain.repository.RpaRobotRepository;
import com.rpa.manage.domain.repository.RpaTaskRepository;
import com.rpa.manage.service.dashboard.DashboardService;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 首页模块实现。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int TASK_STATUS_PENDING = 0;
    private static final int TASK_STATUS_RUNNING = 1;
    private static final int TASK_STATUS_SUCCESS = 2;
    private static final int TASK_STATUS_FAILED = 3;
    private static final int TASK_STATUS_QUEUED = 4;

    private static final int ROBOT_STATUS_OFFLINE = 0;
    private static final int ROBOT_STATUS_ONLINE = 1;
    private static final int ROBOT_STATUS_WORKING = 2;

    private static final int PROCESS_STATUS_DISABLED = 0;
    private static final int PROCESS_STATUS_ENABLED = 1;

    private static final int BUSINESS_DATA_STATUS_AVAILABLE = 1;

    private final RpaTaskRepository rpaTaskRepository;
    private final RpaRobotRepository rpaRobotRepository;
    private final RpaProcessRepository rpaProcessRepository;
    private final DataCollectionRepository dataCollectionRepository;
    private final BusinessDataFinalRepository businessDataFinalRepository;
    private final RpaExecutionRecordRepository rpaExecutionRecordRepository;

    /**
     * 获取首页汇总卡片数据。
     * 统一返回任务、机器人、流程、数据以及系统信息等首页概览内容。
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        long totalTasks = rpaTaskRepository.count();
        long totalRobots = rpaRobotRepository.count();
        long totalProcesses = rpaProcessRepository.count();
        long totalData = dataCollectionRepository.count();

        long todayNewTasks = countTasksCreatedBetween(todayStart, tomorrowStart);
        long onlineRobots = rpaRobotRepository.countByStatus(ROBOT_STATUS_ONLINE);
        long workingRobots = rpaRobotRepository.countByStatus(ROBOT_STATUS_WORKING);
        long offlineRobots = rpaRobotRepository.countByStatus(ROBOT_STATUS_OFFLINE);
        long enabledProcesses = rpaProcessRepository.countByStatus(PROCESS_STATUS_ENABLED);
        long disabledProcesses = rpaProcessRepository.countByStatus(PROCESS_STATUS_DISABLED);
        long todayCollected = countCollectionsBetween(todayStart, tomorrowStart);
        long availableBusinessData = businessDataFinalRepository.countByDataStatus(BUSINESS_DATA_STATUS_AVAILABLE);

        long pendingTasks = rpaTaskRepository.countByStatus(TASK_STATUS_PENDING);
        long runningTasks = rpaTaskRepository.countByStatus(TASK_STATUS_RUNNING);
        long successTasks = rpaTaskRepository.countByStatus(TASK_STATUS_SUCCESS);
        long failedTasks = rpaTaskRepository.countByStatus(TASK_STATUS_FAILED);
        long queuedTasks = rpaTaskRepository.countByStatus(TASK_STATUS_QUEUED);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskSummary", Map.of(
                "total", totalTasks,
                "todayNew", todayNewTasks,
                "pending", pendingTasks,
                "queued", queuedTasks,
                "running", runningTasks,
                "success", successTasks,
                "failed", failedTasks
        ));
        result.put("robotSummary", Map.of(
                "total", totalRobots,
                "online", onlineRobots,
                "working", workingRobots,
                "offline", offlineRobots
        ));
        result.put("processSummary", Map.of(
                "total", totalProcesses,
                "enabled", enabledProcesses,
                "disabled", disabledProcesses
        ));
        result.put("dataSummary", Map.of(
                "total", totalData,
                "todayCollected", todayCollected,
                "available", availableBusinessData
        ));
        result.put("taskStatusOverview", Map.of(
                "pending", pendingTasks,
                "queued", queuedTasks,
                "running", runningTasks,
                "success", successTasks,
                "failed", failedTasks
        ));
        result.put("executionMetrics", buildExecutionMetrics());
        result.put("failureTopReasons", buildFailureTopReasons());
        result.put("systemInfo", buildSystemInfo());
        return result;
    }

    /**
     * 获取首页最近任务列表。
     * 按任务创建时间倒序返回最近若干条任务，并补齐流程和机器人展示信息。
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> recentTasks(Integer limit) {
        int pageSize = limit == null || limit <= 0 ? 5 : Math.min(limit, 20);
        List<RpaTask> tasks = rpaTaskRepository.findAll(
                        PageRequest.of(0, pageSize, Sort.by(Sort.Order.desc("createTime"), Sort.Order.desc("id"))))
                .getContent();
        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<Long, RpaProcess> processMap = rpaProcessRepository.findAllById(tasks.stream()
                        .map(RpaTask::getProcessId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(RpaProcess::getId, Function.identity()));

        Map<Long, RpaRobot> robotMap = rpaRobotRepository.findAllById(tasks.stream()
                        .map(RpaTask::getRobotId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(RpaRobot::getId, Function.identity()));

        return tasks.stream().map(task -> {
            RpaProcess process = processMap.get(task.getProcessId());
            RpaRobot robot = robotMap.get(task.getRobotId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("taskCode", task.getTaskCode());
            item.put("taskName", task.getTaskName());
            item.put("enterpriseName", task.getEnterpriseName());
            item.put("taxpayerIdNo", task.getTaxpayerIdNo());
            item.put("processId", task.getProcessId());
            item.put("processCode", process == null ? "" : process.getProcessCode());
            item.put("processName", process == null ? "" : process.getProcessName());
            item.put("robotId", task.getRobotId());
            item.put("robotCode", robot == null ? "" : robot.getRobotCode());
            item.put("robotName", robot == null ? "" : robot.getRobotName());
            item.put("status", task.getStatus());
            item.put("statusLabel", taskStatusLabel(task.getStatus()));
            item.put("startTime", task.getStartTime());
            item.put("endTime", task.getEndTime());
            item.put("createTime", task.getCreateTime());
            item.put("updateTime", task.getUpdateTime());
            return item;
        }).toList();
    }

    /**
     * 统计指定时间区间内创建的任务数量。
     */
    private long countTasksCreatedBetween(LocalDateTime start, LocalDateTime endExclusive) {
        Specification<RpaTask> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = List.of(
                    cb.greaterThanOrEqualTo(root.get("createTime"), start),
                    cb.lessThan(root.get("createTime"), endExclusive)
            );
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return rpaTaskRepository.count(specification);
    }

    /**
     * 统计指定时间区间内采集完成的数据条数。
     */
    private long countCollectionsBetween(LocalDateTime start, LocalDateTime endExclusive) {
        Specification<DataCollection> specification = (root, ignored, cb) -> {
            List<Predicate> predicates = List.of(
                    cb.greaterThanOrEqualTo(root.get("collectionTime"), start),
                    cb.lessThan(root.get("collectionTime"), endExclusive)
            );
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return dataCollectionRepository.count(specification);
    }

    private Map<String, Object> buildExecutionMetrics() {
        long totalExecutions = rpaExecutionRecordRepository.count();
        long successExecutions = rpaExecutionRecordRepository.countByExecuteStatus(TASK_STATUS_SUCCESS);
        long failedExecutions = rpaExecutionRecordRepository.countByExecuteStatus(TASK_STATUS_FAILED);
        long runningExecutions = rpaExecutionRecordRepository.countByExecuteStatus(TASK_STATUS_RUNNING);
        long queuedExecutions = rpaExecutionRecordRepository.countByExecuteStatus(TASK_STATUS_QUEUED);
        long finishedExecutions = successExecutions + failedExecutions;
        double successRate = finishedExecutions == 0 ? 0 : successExecutions * 100.0 / finishedExecutions;
        Double averageDuration = rpaExecutionRecordRepository.averageDurationSeconds();
        double averageDurationSeconds = averageDuration == null ? 0 : averageDuration;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalExecutions", totalExecutions);
        result.put("successExecutions", successExecutions);
        result.put("failedExecutions", failedExecutions);
        result.put("runningExecutions", runningExecutions);
        result.put("queuedExecutions", queuedExecutions);
        result.put("successRate", roundOneDecimal(successRate));
        result.put("averageDurationSeconds", roundOneDecimal(averageDurationSeconds));
        return result;
    }

    private List<Map<String, Object>> buildFailureTopReasons() {
        return rpaExecutionRecordRepository.findFailureReasonCounts(TASK_STATUS_FAILED, PageRequest.of(0, 5))
                .stream()
                .map(reasonCount -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("reason", normalizeFailureReason(reasonCount.getReason()));
                    item.put("count", reasonCount.getCount());
                    return item;
                })
                .toList();
    }

    private String normalizeFailureReason(String message) {
        if (message == null || message.isBlank()) {
            return "未知错误";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * 组装首页系统信息区域。
     * 目前包含系统版本、运行天数、数据源类型和最近更新时间。
     */
    private Map<String, Object> buildSystemInfo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestCreateTime = earliestCreateTime();
        long runningDays = earliestCreateTime == null ? 0 : Math.max(1, Duration.between(earliestCreateTime, now).toDays());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("systemVersion", "v1.0.0");
        result.put("runningDays", runningDays);
        result.put("dataSource", "MySQL");
        result.put("lastUpdateTime", latestUpdateTime());
        return result;
    }

    /**
     * 取系统内各核心业务表中最早的创建时间，用于估算系统运行天数。
     */
    private LocalDateTime earliestCreateTime() {
        List<LocalDateTime> candidates = List.of(
                firstTaskCreateTime(),
                firstRobotCreateTime(),
                firstProcessCreateTime()
        ).stream().filter(item -> item != null).toList();
        return candidates.isEmpty() ? null : candidates.stream().min(LocalDateTime::compareTo).orElse(null);
    }

    /**
     * 取系统内各核心业务表中最新的更新时间，作为首页系统最近更新时间。
     */
    private LocalDateTime latestUpdateTime() {
        List<LocalDateTime> candidates = List.of(
                latestTaskUpdateTime(),
                latestRobotUpdateTime(),
                latestProcessUpdateTime(),
                latestCollectionUpdateTime()
        ).stream().filter(item -> item != null).toList();
        return candidates.isEmpty() ? null : candidates.stream().max(LocalDateTime::compareTo).orElse(null);
    }

    /**
     * 获取最早创建的任务时间。
     */
    private LocalDateTime firstTaskCreateTime() {
        return rpaTaskRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.asc("createTime"), Sort.Order.asc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(RpaTask::getCreateTime)
                .orElse(null);
    }

    /**
     * 获取最早创建的机器人时间。
     */
    private LocalDateTime firstRobotCreateTime() {
        return rpaRobotRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.asc("createTime"), Sort.Order.asc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(RpaRobot::getCreateTime)
                .orElse(null);
    }

    /**
     * 获取最早创建的流程时间。
     */
    private LocalDateTime firstProcessCreateTime() {
        return rpaProcessRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.asc("createTime"), Sort.Order.asc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(RpaProcess::getCreateTime)
                .orElse(null);
    }

    /**
     * 获取最近更新的任务时间。
     */
    private LocalDateTime latestTaskUpdateTime() {
        return rpaTaskRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("updateTime"), Sort.Order.desc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(RpaTask::getUpdateTime)
                .orElse(null);
    }

    /**
     * 获取最近更新的机器人时间。
     */
    private LocalDateTime latestRobotUpdateTime() {
        return rpaRobotRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("updateTime"), Sort.Order.desc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(RpaRobot::getUpdateTime)
                .orElse(null);
    }

    /**
     * 获取最近更新的流程时间。
     */
    private LocalDateTime latestProcessUpdateTime() {
        return rpaProcessRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("updateTime"), Sort.Order.desc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(RpaProcess::getUpdateTime)
                .orElse(null);
    }

    /**
     * 获取最近更新的数据采集记录时间。
     */
    private LocalDateTime latestCollectionUpdateTime() {
        return dataCollectionRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Order.desc("updateTime"), Sort.Order.desc("id"))))
                .getContent()
                .stream()
                .findFirst()
                .map(DataCollection::getUpdateTime)
                .orElse(null);
    }

    /**
     * 将任务状态码转换为首页展示文本。
     */
    private String taskStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case TASK_STATUS_PENDING -> "待执行";
            case TASK_STATUS_RUNNING -> "运行中";
            case TASK_STATUS_SUCCESS -> "已完成";
            case TASK_STATUS_FAILED -> "失败";
            case TASK_STATUS_QUEUED -> "排队中";
            default -> String.valueOf(status);
        };
    }
}
