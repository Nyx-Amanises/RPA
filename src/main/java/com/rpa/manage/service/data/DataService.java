package com.rpa.manage.service.data;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.domain.dto.data.BusinessDataPageQuery;
import com.rpa.manage.domain.dto.data.DataAnalysisPageQuery;
import com.rpa.manage.domain.dto.data.DataCollectionCreateRequest;
import com.rpa.manage.domain.dto.data.DataCollectionPageQuery;
import com.rpa.manage.domain.dto.data.DataProcessingPageQuery;
import java.util.Map;

/**
 * 数据管理模块服务接口。
 *
 * <p>这个接口覆盖从采集到最终业务数据查询的整条数据链路，
 * 返回值多数都是已经按页面展示需求组装好的结果结构。
 */
public interface DataService {

    /**
     * 分页查询数据采集记录。
     *
     * @param query 查询条件
     * @return 采集记录分页结果和汇总信息
     */
    Map<String, Object> collectionPage(DataCollectionPageQuery query);

    /**
     * 查询数据采集详情。
     *
     * @param id 采集记录 ID
     * @return 采集详情
     */
    Map<String, Object> collectionDetail(Long id);

    /**
     * 新增数据采集记录。
     *
     * @param request 采集新增请求
     * @return 新建后的采集记录
     */
    Map<String, Object> createCollection(DataCollectionCreateRequest request);

    /**
     * 删除数据采集记录。
     *
     * @param id 采集记录 ID
     * @return 删除结果
     */
    Map<String, Object> deleteCollection(Long id);

    /**
     * 分页查询数据解析记录。
     *
     * @param query 查询条件
     * @return 解析记录分页结果和汇总信息
     */
    Map<String, Object> analysisPage(DataAnalysisPageQuery query);

    /**
     * 查询数据解析详情。
     *
     * @param id 解析记录 ID
     * @return 解析详情
     */
    Map<String, Object> analysisDetail(Long id);

    /**
     * 删除数据解析记录。
     *
     * @param id 解析记录 ID
     * @return 删除结果
     */
    Map<String, Object> deleteAnalysis(Long id);

    /**
     * 分页查询数据加工记录。
     *
     * @param query 查询条件
     * @return 加工记录分页结果和汇总信息
     */
    Map<String, Object> processingPage(DataProcessingPageQuery query);

    /**
     * 查询数据加工详情。
     *
     * @param id 加工记录 ID
     * @return 加工详情
     */
    Map<String, Object> processingDetail(Long id);

    /**
     * 删除数据加工记录。
     *
     * @param id 加工记录 ID
     * @return 删除结果
     */
    Map<String, Object> deleteProcessing(Long id);

    /**
     * 分页查询最终业务数据。
     *
     * @param query 查询条件
     * @return 业务数据分页结果
     */
    PageResult<Map<String, Object>> businessPage(BusinessDataPageQuery query);

    /**
     * 查询业务数据详情。
     *
     * @param id 业务数据 ID
     * @return 业务数据详情
     */
    Map<String, Object> businessDetail(Long id);

    /**
     * 删除最终业务数据。
     *
     * @param id 业务数据 ID
     * @return 删除结果
     */
    Map<String, Object> deleteBusiness(Long id);
}
