package com.rpa.manage.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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

@Getter
@Setter
@Entity
@Table(name = "rpa_process_version_step")
public class RpaProcessVersionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_version_id", nullable = false)
    private Long processVersionId;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "step_type", nullable = false, length = 50)
    private String stepType;

    @Column(name = "script_lang", nullable = false, length = 30)
    private String scriptLang;

    @Column(name = "script_content", columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds;

    @Column(name = "failure_strategy", nullable = false, length = 20)
    private String failureStrategy;

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
