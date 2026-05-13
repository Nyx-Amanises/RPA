package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.IndicatorQuotaRuleIndicator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndicatorQuotaRuleIndicatorRepository extends JpaRepository<IndicatorQuotaRuleIndicator, Long> {

    boolean existsByIndicatorId(Long indicatorId);

    List<IndicatorQuotaRuleIndicator> findByQuotaRuleIdOrderBySortNoAscIdAsc(Long quotaRuleId);

    void deleteByQuotaRuleId(Long quotaRuleId);
}
