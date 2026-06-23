package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
