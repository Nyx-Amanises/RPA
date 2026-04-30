package com.rpa.manage.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@Entity
@Table(name = "rpa_execution_record")
public class RpaExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_code", nullable = false, length = 50, unique = true)
    private String executionCode;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "process_version_id")
    private Long processVersionId;

    @Column(name = "process_version_no")
    private Integer processVersionNo;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(name = "execute_status", nullable = false)
    private Integer executeStatus;

    @Column(name = "start_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "screenshot_url", length = 255)
    private String screenshotUrl;

    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "create_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
