package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.IndicatorDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndicatorDefinitionRepository extends JpaRepository<IndicatorDefinition, Long>, JpaSpecificationExecutor<IndicatorDefinition> {

    boolean existsByIndicatorCode(String indicatorCode);

    boolean existsByIndicatorCodeAndIdNot(String indicatorCode, Long id);

    Optional<IndicatorDefinition> findByIndicatorCode(String indicatorCode);

    List<IndicatorDefinition> findByIdIn(List<Long> ids);
}
