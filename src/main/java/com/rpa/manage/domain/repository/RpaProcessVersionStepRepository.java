package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaProcessVersionStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RpaProcessVersionStepRepository extends JpaRepository<RpaProcessVersionStep, Long> {

    List<RpaProcessVersionStep> findByProcessVersionIdOrderByStepNoAsc(Long processVersionId);

    void deleteByProcessId(Long processId);
}
