package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaRobot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RpaRobotRepository extends JpaRepository<RpaRobot, Long>, JpaSpecificationExecutor<RpaRobot> {

    boolean existsByRobotCode(String robotCode);

    @Query(value = "select * from rpa_robot where id = :id for update", nativeQuery = true)
    Optional<RpaRobot> findByIdForUpdate(@Param("id") Long id);

    boolean existsByRobotCodeAndIdNot(String robotCode, Long id);

    long countByStatus(Integer status);
}
