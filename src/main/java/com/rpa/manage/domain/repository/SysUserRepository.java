package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.SysUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysUserRepository extends JpaRepository<SysUser, Long>, JpaSpecificationExecutor<SysUser> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByMobile(String mobile);

    boolean existsByMobileAndIdNot(String mobile, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<SysUser> findAllByRoleId(Long roleId);

    long countByRoleId(Long roleId);
}
