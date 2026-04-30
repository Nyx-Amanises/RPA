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
@Table(name = "data_collection")
public class DataCollection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "taxpayer_id_no", nullable = false, length = 30)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 120)
    private String enterpriseName;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "collect_time", nullable = false)
    private LocalDateTime collectTime;

    @Column(name = "collection_time", nullable = false)
    private LocalDateTime collectionTime;
}
