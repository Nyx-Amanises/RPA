package com.rpa.manage.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "indicator_quota_result")
public class IndicatorQuotaResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quota_rule_id", nullable = false)
    private Long quotaRuleId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "taxpayer_id_no", nullable = false, length = 32)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 120)
    private String enterpriseName;

    @Column(name = "indicator_result_ids", columnDefinition = "LONGTEXT")
    private String indicatorResultIds;

    @Column(name = "indicator_values", columnDefinition = "LONGTEXT")
    private String indicatorValues;

    @Column(name = "result_var_name", nullable = false, length = 50)
    private String resultVarName;

    @Column(name = "result_type", nullable = false, length = 20)
    private String resultType;

    @Column(name = "calculated_value_decimal", precision = 24, scale = 6)
    private BigDecimal calculatedValueDecimal;

    @Column(name = "calculated_value_text", length = 500)
    private String calculatedValueText;

    @Column(name = "calculated_value_json", columnDefinition = "LONGTEXT")
    private String calculatedValueJson;

    @Column(name = "output_data", columnDefinition = "LONGTEXT")
    private String outputData;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "calculate_time", nullable = false)
    private LocalDateTime calculateTime;
}
