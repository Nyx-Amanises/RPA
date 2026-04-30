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
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", nullable = false, length = 20)
    private String realName;

    @Column(length = 64)
    private String email;

    @Column(nullable = false, length = 11, unique = true)
    private String mobile;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "last_login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Column(length = 255)
    private String remark;

    @Column(name = "role_id")
    private Long roleId;

    @Column(nullable = false)
    private Integer status;
}
