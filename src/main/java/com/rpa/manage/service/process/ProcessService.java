package com.rpa.manage.service.process;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.domain.dto.process.ProcessDesignRequest;
import com.rpa.manage.domain.dto.process.ProcessPageQuery;
import com.rpa.manage.domain.dto.process.ProcessUpsertRequest;
import java.util.Map;

/**
 * 流程管理服务接口。
 *
 * <p>流程模块除了基础的增删改查，还包含“流程设计”能力，
 * 所以接口中会出现基础信息和设计信息两套方法。
 */
public interface ProcessService {

    /**
     * 分页查询流程列表。
     *
     * @param query 查询条件
     * @return 流程分页结果
     */
    PageResult<Map<String, Object>> page(ProcessPageQuery query);

    /**
     * 新增流程。
     *
     * @param request 流程新增请求
     * @return 新建后的流程信息
     */
    Map<String, Object> create(ProcessUpsertRequest request);

    /**
     * 查询流程基础详情。
     *
     * @param id 流程 ID
     * @return 流程详情
     */
    Map<String, Object> detail(Long id);

    /**
     * 查询流程设计详情。
     *
     * @param id 流程 ID
     * @return 流程设计数据
     */
    Map<String, Object> designDetail(Long id);

    /**
     * 修改指定流程。
     *
     * @param id 流程 ID
     * @param request 流程修改请求
     * @return 修改后的流程信息
     */
    Map<String, Object> update(Long id, ProcessUpsertRequest request);

    /**
     * 保存流程设计。
     *
     * @param id 流程 ID
     * @param request 流程设计请求
     * @return 保存结果
     */
    Map<String, Object> saveDesign(Long id, ProcessDesignRequest request);

    /**
     * 发布当前流程草稿，生成一份可执行的版本快照。
     *
     * @param id 流程 ID
     * @return 发布后的流程设计信息
     */
    Map<String, Object> publish(Long id);

    /**
     * 停用当前已发布版本。
     *
     * @param id 流程 ID
     * @return 停用后的流程信息
     */
    Map<String, Object> disablePublishedVersion(Long id);

    /**
     * 删除指定流程。
     *
     * @param id 流程 ID
     * @return 删除结果
     */
    Map<String, Object> delete(Long id);
}
