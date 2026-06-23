package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);
    List<User> findByOrganizationId(Long organizationId);
}
