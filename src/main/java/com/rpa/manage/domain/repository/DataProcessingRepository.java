package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.DataProcessing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataProcessingRepository extends JpaRepository<DataProcessing, Long>, JpaSpecificationExecutor<DataProcessing> {

    long countByStatus(Integer status);

    List<DataProcessing> findByExecutionIdOrderByIdAsc(Long executionId);
}
