package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.SysUserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    List<SysUserRole> findAllByUserId(Long userId);

    List<SysUserRole> findAllByRoleId(Long roleId);

    void deleteByUserId(Long userId);

    long countByRoleId(Long roleId);
}
