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
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
@Entity
@Table(name = "rpa_task")
public class RpaTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false, length = 50, unique = true)
    private String taskCode;

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @Column(name = "taxpayer_id_no", nullable = false, length = 30)
    private String taxpayerIdNo;

    @Column(name = "enterprise_name", nullable = false, length = 120)
    private String enterpriseName;

    @Column(name = "process_id", nullable = false)
    private Long processId;

    @Column(name = "robot_id", nullable = false)
    private Long robotId;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Column(name = "creator_user_id")
    private Long creatorUserId;

    @Column(length = 255)
    private String remark;
}
