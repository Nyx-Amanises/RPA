package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RpaTaskRepository extends JpaRepository<RpaTask, Long>, JpaSpecificationExecutor<RpaTask> {

    boolean existsByTaskCode(String taskCode);

    @Query(value = "select * from rpa_task where id = :id for update", nativeQuery = true)
    Optional<RpaTask> findByIdForUpdate(@Param("id") Long id);

    List<RpaTask> findTop10ByOrderByCreateTimeDesc();

    long countByStatus(Integer status);

    long countByProcessId(Long processId);

    long countByRobotId(Long robotId);
}
