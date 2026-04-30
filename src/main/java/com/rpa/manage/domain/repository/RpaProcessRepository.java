package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RpaProcessRepository extends JpaRepository<RpaProcess, Long>, JpaSpecificationExecutor<RpaProcess> {

    boolean existsByProcessCode(String processCode);

    boolean existsByProcessCodeAndIdNot(String processCode, Long id);

    long countByStatus(Integer status);
}
