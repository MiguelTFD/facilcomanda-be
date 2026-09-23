package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Organization;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Organization o WHERE o.id = :id")
    Optional<Organization> findLockedById(@Param("id") Long id);
}
