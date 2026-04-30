package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaProcessStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RpaProcessStepRepository extends JpaRepository<RpaProcessStep, Long> {

    List<RpaProcessStep> findByProcessIdOrderByStepNoAsc(Long processId);

    void deleteByProcessId(Long processId);
}
