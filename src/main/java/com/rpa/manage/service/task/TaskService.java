package com.rpa.manage.service.task;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.domain.dto.task.ExecutionPageQuery;
import com.rpa.manage.domain.dto.task.TaskPageQuery;
import com.rpa.manage.domain.dto.task.TaskUpsertRequest;
import java.util.Map;

/**
 * 任务管理模块服务接口。
 *
 * <p>任务模块除了常规的增删改查，还包含“立即执行”和“执行记录查询”两类能力，
 * 所以它既服务任务页面，也服务执行记录页面。
 */
public interface TaskService {

    /**
     * 分页查询任务列表。
     *
     * @param query 查询条件
     * @return 任务分页结果
     */
    PageResult<Map<String, Object>> page(TaskPageQuery query);

    /**
     * 新增任务。
     *
     * @param request 任务新增请求
     * @return 新建后的任务信息
     */
    Map<String, Object> create(TaskUpsertRequest request);

    /**
     * 查询任务详情。
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    Map<String, Object> detail(Long id);

    /**
     * 修改指定任务。
     *
     * @param id 任务 ID
     * @param request 任务修改请求
     * @return 修改后的任务信息
     */
    Map<String, Object> update(Long id, TaskUpsertRequest request);

    /**
     * 立即执行指定任务。
     *
     * @param id 任务 ID
     * @return 执行结果
     */
    Map<String, Object> execute(Long id);

    /**
     * 删除指定任务。
     *
     * @param id 任务 ID
     * @return 删除结果
     */
    Map<String, Object> delete(Long id);

    /**
     * 分页查询执行记录。
     *
     * @param query 查询条件
     * @return 执行记录分页结果
     */
    PageResult<Map<String, Object>> executionPage(ExecutionPageQuery query);

    /**
     * 查询执行记录详情。
     *
     * @param id 执行记录 ID
     * @return 执行记录详情
     */
    Map<String, Object> executionDetail(Long id);
}
