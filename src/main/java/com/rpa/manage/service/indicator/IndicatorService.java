package com.rpa.manage.service.indicator;

import com.rpa.manage.common.api.PageResult;
import com.rpa.manage.domain.dto.indicator.IndicatorPageQuery;
import com.rpa.manage.domain.dto.indicator.IndicatorUpsertRequest;
import com.rpa.manage.domain.dto.indicator.QuotaCalculateRequest;
import com.rpa.manage.domain.dto.indicator.QuotaRulePageQuery;
import com.rpa.manage.domain.dto.indicator.QuotaRuleUpsertRequest;
import java.util.List;
import java.util.Map;

public interface IndicatorService {

    PageResult<Map<String, Object>> indicatorPage(IndicatorPageQuery query);

    List<Map<String, Object>> indicatorOptions();

    Map<String, Object> indicatorDetail(Long id);

    Map<String, Object> createIndicator(IndicatorUpsertRequest request);

    Map<String, Object> updateIndicator(Long id, IndicatorUpsertRequest request);

    Map<String, Object> deleteIndicator(Long id);

    Map<String, Object> calculateIndicator(Long id);

    PageResult<Map<String, Object>> quotaRulePage(QuotaRulePageQuery query);

    Map<String, Object> quotaRuleDetail(Long id);

    Map<String, Object> createQuotaRule(QuotaRuleUpsertRequest request);

    Map<String, Object> updateQuotaRule(Long id, QuotaRuleUpsertRequest request);

    Map<String, Object> deleteQuotaRule(Long id);

    Map<String, Object> calculateQuotaRule(Long id, QuotaCalculateRequest request);
}
