package com.rpa.manage.service.dashboard;

import java.util.List;
import java.util.Map;

/**
 * 首页模块服务接口。
 *
 * <p>这里定义首页所需的聚合型数据能力。
 * 这类接口通常面向“直接展示”，所以返回值大多是已经组装好的 Map 结构。
 */
public interface DashboardService {

    /**
     * 获取首页汇总信息。
     *
     * @return 首页统计数据
     */
    Map<String, Object> summary();

    /**
     * 获取最近任务列表。
     *
     * @param limit 期望返回条数
     * @return 最近任务列表
     */
    List<Map<String, Object>> recentTasks(Integer limit);
}
