package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Order> findByOrganizationId(Long organizationId);
    boolean existsByIdempotencyKeyAndOrganizationId(String idempotencyKey, Long organizationId);
}
