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
@Table(name = "data_processing")
public class DataProcessing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "taxpayer_id_no", nullable = false, length = 32)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 100)
    private String enterpriseName;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "validation_result", columnDefinition = "LONGTEXT")
    private String validationResult;

    @Column(name = "processed_data", columnDefinition = "LONGTEXT")
    private String processedData;

    @Column(name = "validation_detail", columnDefinition = "LONGTEXT")
    private String validationDetail;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "process_time", nullable = false)
    private LocalDateTime processTime;

    @Column(name = "processing_time", nullable = false)
    private LocalDateTime processingTime;
}
