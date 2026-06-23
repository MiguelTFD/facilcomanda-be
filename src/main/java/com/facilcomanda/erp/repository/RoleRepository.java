package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Role> findByOrganizationId(Long organizationId);
}
