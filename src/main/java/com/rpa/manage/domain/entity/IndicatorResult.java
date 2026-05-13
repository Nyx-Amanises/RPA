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
@Table(name = "indicator_result")
public class IndicatorResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_id", nullable = false)
    private Long indicatorId;

    @Column(name = "indicator_code", nullable = false, length = 50)
    private String indicatorCode;

    @Column(name = "result_var_name", length = 50)
    private String resultVarName;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "taxpayer_id_no", nullable = false, length = 32)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 120)
    private String enterpriseName;

    @Column(name = "result_type", nullable = false, length = 20)
    private String resultType;

    @Column(name = "result_value_decimal", precision = 24, scale = 6)
    private BigDecimal resultValueDecimal;

    @Column(name = "result_value_text", length = 500)
    private String resultValueText;

    @Column(name = "result_value_json", columnDefinition = "LONGTEXT")
    private String resultValueJson;

    @Column(name = "calculation_context", columnDefinition = "LONGTEXT")
    private String calculationContext;

    @Column(name = "source_data_ref", columnDefinition = "LONGTEXT")
    private String sourceDataRef;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "calculate_time", nullable = false)
    private LocalDateTime calculateTime;
}
