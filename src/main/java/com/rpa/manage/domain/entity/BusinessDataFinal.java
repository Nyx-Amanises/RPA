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
@Table(name = "data_business_final")
public class BusinessDataFinal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "processing_id", nullable = false)
    private Long processingId;

    @Column(name = "taxpayer_id_no", nullable = false, length = 30)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 120)
    private String enterpriseName;

    @Column(name = "tax_area_id", length = 30)
    private String taxAreaId;

    @Column(name = "indicator_code", length = 64)
    private String indicatorCode;

    @Column(name = "tax_area_name", length = 100)
    private String taxAreaName;

    @Column(name = "data_status", nullable = false)
    private Integer dataStatus;

    @Column(name = "business_data", columnDefinition = "LONGTEXT")
    private String businessData;
}
