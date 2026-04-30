package com.rpa.manage.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "data_analysis")
public class DataAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "taxpayer_id_no", nullable = false, length = 32)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 100)
    private String enterpriseName;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "rule_name", length = 100)
    private String ruleName;

    @Column(name = "extracted_field_count")
    private Integer extractedFieldCount;

    @Column(name = "parsed_data", columnDefinition = "LONGTEXT")
    private String parsedData;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "analysis_time", nullable = false)
    private LocalDateTime analysisTime;
}
