package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.DataCollection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataCollectionRepository extends JpaRepository<DataCollection, Long>, JpaSpecificationExecutor<DataCollection> {

    long countByStatus(Integer status);

    boolean existsByExecutionId(Long executionId);

    List<DataCollection> findByExecutionIdOrderByIdAsc(Long executionId);
}
