package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.IndicatorResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndicatorResultRepository extends JpaRepository<IndicatorResult, Long>, JpaSpecificationExecutor<IndicatorResult> {

    Optional<IndicatorResult> findTopByIndicatorIdAndStatusOrderByCalculateTimeDescIdDesc(Long indicatorId, Integer status);

    List<IndicatorResult> findByIndicatorIdInAndStatusOrderByCalculateTimeDescIdDesc(List<Long> indicatorIds, Integer status);

    Optional<IndicatorResult> findTopByIndicatorIdAndTaxpayerIdNoAndStatusOrderByCalculateTimeDescIdDesc(
            Long indicatorId,
            String taxpayerIdNo,
            Integer status
    );

    Optional<IndicatorResult> findTopByIndicatorIdAndEnterpriseNameAndStatusOrderByCalculateTimeDescIdDesc(
            Long indicatorId,
            String enterpriseName,
            Integer status
    );

    Optional<IndicatorResult> findByIndicatorIdAndExecutionId(Long indicatorId, Long executionId);

    List<IndicatorResult> findByExecutionIdAndIndicatorIdIn(Long executionId, List<Long> indicatorIds);
}
