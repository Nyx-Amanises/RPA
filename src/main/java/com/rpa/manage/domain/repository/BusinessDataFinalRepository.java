package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.BusinessDataFinal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessDataFinalRepository extends JpaRepository<BusinessDataFinal, Long>, JpaSpecificationExecutor<BusinessDataFinal> {

    long countByDataStatus(Integer dataStatus);

    List<BusinessDataFinal> findByExecutionIdOrderByIdAsc(Long executionId);
}
