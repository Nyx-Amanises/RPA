package com.rpa.manage.service.robot;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.domain.dto.robot.RobotPageQuery;
import com.rpa.manage.domain.dto.robot.RobotUpsertRequest;
import java.util.Map;

/**
 * 机器人管理服务接口。
 *
 * <p>机器人是任务执行时的重要运行资源，
 * 因此这里既要提供列表维护能力，也要提供概览统计能力。
 */
public interface RobotService {

    /**
     * 查询机器人概览统计。
     *
     * @return 机器人统计信息
     */
    Map<String, Object> overview();

    /**
     * 分页查询机器人列表。
     *
     * @param query 查询条件
     * @return 机器人分页结果
     */
    PageResult<Map<String, Object>> page(RobotPageQuery query);

    /**
     * 新增机器人。
     *
     * @param request 机器人新增请求
     * @return 新建后的机器人信息
     */
    Map<String, Object> create(RobotUpsertRequest request);

    /**
     * 查询机器人详情。
     *
     * @param id 机器人 ID
     * @return 机器人详情
     */
    Map<String, Object> detail(Long id);

    /**
     * 修改机器人。
     *
     * @param id 机器人 ID
     * @param request 机器人修改请求
     * @return 修改后的机器人信息
     */
    Map<String, Object> update(Long id, RobotUpsertRequest request);

    /**
     * 删除机器人。
     *
     * @param id 机器人 ID
     * @return 删除结果
     */
    Map<String, Object> delete(Long id);
}
