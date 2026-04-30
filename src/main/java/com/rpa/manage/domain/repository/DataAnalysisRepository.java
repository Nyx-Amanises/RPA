package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.DataAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataAnalysisRepository extends JpaRepository<DataAnalysis, Long>, JpaSpecificationExecutor<DataAnalysis> {

    long countByStatus(Integer status);

    List<DataAnalysis> findByExecutionIdOrderByIdAsc(Long executionId);
}
