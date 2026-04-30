package com.rpa.manage.common.util;

import com.rpa.manage.common.api.PageResult;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 骨架版项目统一使用的占位数据工具。
 * 当前阶段我们只保留接口结构、参数定义和分层关系，
 * 因此 service impl 不再承载真实业务逻辑，而是通过该工具返回
 * 规范化的占位结果，方便后续逐个接口补充实现。
 */
public final class SkeletonSupport {

    private SkeletonSupport() {
    }

    /**
     * 构造一个简单对象结果，用于详情、新增、修改、删除这类接口。
     *
     * @param module 模块名称，便于前后端联调时快速识别接口归属
     * @param action 当前动作，例如 page/detail/create/update
     * @return 统一结构的占位数据
     */
    public static Map<String, Object> object(String module, String action) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", module);
        data.put("action", action);
        data.put("implemented", false);
        data.put("message", "当前为项目骨架版本，具体业务逻辑待实现");
        data.put("timestamp", LocalDateTime.now());
        return data;
    }

    /**
     * 构造一个空分页结果，用于分页查询类接口。
     *
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @return 空列表分页对象
     */
    public static <T> PageResult<T> emptyPage(Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        return new PageResult<>(0, safePageNum, safePageSize, List.of());
    }

    /**
     * 构造一个空树或空数组结果，用于资源树、最近任务等集合型接口。
     *
     * @return 空集合
     */
    public static <T> List<T> emptyList() {
        return List.of();
    }
}
