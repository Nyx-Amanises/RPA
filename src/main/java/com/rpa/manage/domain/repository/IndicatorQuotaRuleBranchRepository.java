package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.IndicatorQuotaRuleBranch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndicatorQuotaRuleBranchRepository extends JpaRepository<IndicatorQuotaRuleBranch, Long> {

    List<IndicatorQuotaRuleBranch> findByQuotaRuleIdOrderByBranchOrderAscIdAsc(Long quotaRuleId);

    void deleteByQuotaRuleId(Long quotaRuleId);
}
