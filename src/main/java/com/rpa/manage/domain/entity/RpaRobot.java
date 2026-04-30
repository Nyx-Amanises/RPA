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
@Table(name = "rpa_robot")
public class RpaRobot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "robot_code", nullable = false, length = 50, unique = true)
    private String robotCode;

    @Column(name = "robot_name", nullable = false, length = 100)
    private String robotName;

    @Column(name = "robot_type", nullable = false, length = 50)
    private String robotType;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "current_task_id")
    private Long currentTaskId;

    @Column(name = "last_heartbeat_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeatTime;
}
