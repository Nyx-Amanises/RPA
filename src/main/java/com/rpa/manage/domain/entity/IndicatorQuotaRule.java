package com.rpa.manage.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "indicator_quota_rule")
public class IndicatorQuotaRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quota_name", nullable = false, length = 100)
    private String quotaName;

    @Column(name = "result_var_name", nullable = false, length = 50)
    private String resultVarName;

    @Column(name = "output_template", nullable = false, columnDefinition = "LONGTEXT")
    private String outputTemplate;

    @Column(name = "latest_result_text", length = 500)
    private String latestResultText;

    @Column(name = "latest_result_id")
    private Long latestResultId;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 255)
    private String remark;
}
