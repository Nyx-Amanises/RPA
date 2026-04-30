package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.RpaProcessVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RpaProcessVersionRepository extends JpaRepository<RpaProcessVersion, Long> {

    List<RpaProcessVersion> findByProcessIdOrderByVersionNoDesc(Long processId);

    List<RpaProcessVersion> findByProcessIdAndVersionStatus(Long processId, Integer versionStatus);

    Optional<RpaProcessVersion> findFirstByProcessIdOrderByVersionNoDesc(Long processId);

    void deleteByProcessId(Long processId);
}
