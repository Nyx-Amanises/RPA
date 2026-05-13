package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.IndicatorQuotaResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndicatorQuotaResultRepository extends JpaRepository<IndicatorQuotaResult, Long>, JpaSpecificationExecutor<IndicatorQuotaResult> {
}
