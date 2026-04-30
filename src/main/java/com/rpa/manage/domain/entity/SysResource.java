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
@Table(name = "sys_resource")
public class SysResource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "resource_name", nullable = false, length = 100)
    private String resourceName;

    @Column(name = "resource_code", nullable = false, length = 50, unique = true)
    private String resourceCode;

    @Column(name = "resource_type", nullable = false)
    private Integer resourceType;

    @Column(length = 120)
    private String path;

    @Column(length = 120)
    private String component;

    @Column(name = "permission_key", length = 100)
    private String permissionKey;

    @Column(name = "permission_code", length = 120, unique = true)
    private String permissionCode;

    @Column(length = 64)
    private String icon;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Integer status;
}
