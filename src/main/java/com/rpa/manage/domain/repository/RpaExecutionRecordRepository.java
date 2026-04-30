package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaExecutionRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RpaExecutionRecordRepository extends JpaRepository<RpaExecutionRecord, Long>, JpaSpecificationExecutor<RpaExecutionRecord> {

    Optional<RpaExecutionRecord> findTopByTaskIdOrderByStartTimeDesc(Long taskId);

    long countByExecuteStatus(Integer executeStatus);

    @Query("select avg(e.durationSeconds) from RpaExecutionRecord e where e.durationSeconds is not null")
    Double averageDurationSeconds();

    @Query("""
            select e.errorMessage as reason, count(e.id) as count
            from RpaExecutionRecord e
            where e.executeStatus = :executeStatus
            group by e.errorMessage
            order by count(e.id) desc
            """)
    List<FailureReasonCount> findFailureReasonCounts(@Param("executeStatus") Integer executeStatus, Pageable pageable);

    interface FailureReasonCount {
        String getReason();

        Long getCount();
    }
}
