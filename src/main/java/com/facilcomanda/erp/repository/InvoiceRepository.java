package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Invoice> findByOrder_IdAndOrganizationId(Long orderId, Long organizationId);
    List<Invoice> findByOrganizationIdOrderByPaidAtDesc(Long organizationId);
    boolean existsByOrder_IdAndOrganizationId(Long orderId, Long organizationId);
}
