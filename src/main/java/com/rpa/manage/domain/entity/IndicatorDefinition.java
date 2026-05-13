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
@Table(name = "indicator_definition")
public class IndicatorDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;

    @Column(name = "indicator_code", nullable = false, length = 50, unique = true)
    private String indicatorCode;

    @Column(name = "result_var_name", length = 50)
    private String resultVarName;

    @Column(name = "indicator_logic", nullable = false, length = 500)
    private String indicatorLogic;

    @Column(name = "formula_expression", nullable = false, columnDefinition = "LONGTEXT")
    private String formulaExpression;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 255)
    private String remark;
}
