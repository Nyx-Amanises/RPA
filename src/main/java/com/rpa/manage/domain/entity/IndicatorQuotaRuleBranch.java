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
@Table(name = "indicator_quota_rule_branch")
public class IndicatorQuotaRuleBranch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quota_rule_id", nullable = false)
    private Long quotaRuleId;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "condition_expression", columnDefinition = "LONGTEXT")
    private String conditionExpression;

    @Column(name = "calculation_expression", nullable = false, columnDefinition = "LONGTEXT")
    private String calculationExpression;

    @Column(name = "branch_order", nullable = false)
    private Integer branchOrder;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultBranch;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 255)
    private String remark;
}
