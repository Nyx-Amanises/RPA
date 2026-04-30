package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.SysRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysRoleRepository extends JpaRepository<SysRole, Long>, JpaSpecificationExecutor<SysRole> {

    Optional<SysRole> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleCodeAndIdNot(String roleCode, Long id);

    boolean existsByRoleName(String roleName);

    boolean existsByRoleNameAndIdNot(String roleName, Long id);
}
