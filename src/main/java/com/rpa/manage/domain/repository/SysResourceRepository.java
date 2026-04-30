package com.rpa.manage.domain.repository;

import com.rpa.manage.domain.entity.SysResource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysResourceRepository extends JpaRepository<SysResource, Long>, JpaSpecificationExecutor<SysResource> {

    List<SysResource> findAllByOrderBySortNoAscIdAsc();

    boolean existsByResourceCode(String resourceCode);

    boolean existsByResourceCodeAndIdNot(String resourceCode, Long id);

    boolean existsByParentId(Long parentId);

    boolean existsByPermissionKey(String permissionKey);

    boolean existsByPermissionKeyAndIdNot(String permissionKey, Long id);

    boolean existsByPermissionCode(String permissionCode);

    boolean existsByPermissionCodeAndIdNot(String permissionCode, Long id);
}
