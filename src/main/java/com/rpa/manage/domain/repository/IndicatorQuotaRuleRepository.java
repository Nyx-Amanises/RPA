package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.IndicatorQuotaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndicatorQuotaRuleRepository extends JpaRepository<IndicatorQuotaRule, Long>, JpaSpecificationExecutor<IndicatorQuotaRule> {
}
