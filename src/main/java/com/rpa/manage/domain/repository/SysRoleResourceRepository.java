package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.SysRoleResource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysRoleResourceRepository extends JpaRepository<SysRoleResource, Long> {

    List<SysRoleResource> findAllByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

    boolean existsByResourceId(Long resourceId);
}
