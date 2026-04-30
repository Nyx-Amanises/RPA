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
@Table(name = "sys_dict_data")
public class SysDictData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", nullable = false, length = 50)
    private String typeCode;

    @Column(name = "dict_label", nullable = false, length = 50)
    private String dictLabel;

    @Column(name = "dict_value", nullable = false, length = 50)
    private String dictValue;

    @Column(nullable = false)
    private Integer status;
}
