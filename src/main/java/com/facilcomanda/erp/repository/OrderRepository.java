package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.TableState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Order> findByOrganizationId(Long organizationId);
    boolean existsByIdempotencyKeyAndOrganizationId(String idempotencyKey, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findLockedByIdAndOrganizationId(Long id, Long organizationId);

    List<Order> findByOrganizationIdAndStatusInAndRestaurantTable_State(Long organizationId,
            List<OrderStatus> statuses,
            TableState tableState);
}
