package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.dto.ProductQuantityResponse;
import com.facilcomanda.erp.model.Invoice;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Invoice> findByOrder_IdAndOrganizationId(Long orderId, Long organizationId);
    List<Invoice> findByOrganizationIdOrderByPaidAtDesc(Long organizationId);
    boolean existsByOrder_IdAndOrganizationId(Long orderId, Long organizationId);

    // --- Agregaciones del resumen de reportes (feature 022) ---
    // El período es semiabierto: paidAt >= :from y paidAt < :toExclusive.

    /** Órdenes cobradas en el período. */
    @Query("select count(i) from Invoice i "
            + "where i.organizationId = :organizationId "
            + "and i.paidAt >= :from and i.paidAt < :toExclusive")
    long countInvoicesInPeriod(@Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /** Cobrado por método, tal como se registró en {@code invoice_payments}. */
    @Query("select new com.facilcomanda.erp.repository.MethodTotal(p.method, sum(p.amount)) "
            + "from InvoicePayment p join p.invoice i "
            + "where i.organizationId = :organizationId and p.organizationId = :organizationId "
            + "and i.paidAt >= :from and i.paidAt < :toExclusive "
            + "group by p.method")
    List<MethodTotal> sumPaymentsByMethodInPeriod(@Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Vuelto entregado por las facturas que sí tienen filas de pago. Sale solo
     * del efectivo (decisión de la feature 020), por lo que el resumen lo
     * descuenta del EFECTIVO cobrado.
     */
    @Query("select coalesce(sum(i.changeAmount), 0) from Invoice i "
            + "where i.organizationId = :organizationId "
            + "and i.paidAt >= :from and i.paidAt < :toExclusive "
            + "and exists (select p.id from InvoicePayment p where p.invoice = i)")
    BigDecimal sumChangeAmountWithPaymentsInPeriod(@Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /**
     * Facturas históricas anteriores a la feature 020, sin filas de pago: todas
     * fueron en efectivo y cuentan por su {@code orderTotal}.
     */
    @Query("select coalesce(sum(i.orderTotal), 0) from Invoice i "
            + "where i.organizationId = :organizationId "
            + "and i.paidAt >= :from and i.paidAt < :toExclusive "
            + "and not exists (select p.id from InvoicePayment p where p.invoice = i)")
    BigDecimal sumOrderTotalWithoutPaymentsInPeriod(@Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    /** Cantidad vendida por producto en el período, de mayor a menor. */
    @Query("select new com.facilcomanda.erp.dto.ProductQuantityResponse(oi.product.name, sum(oi.quantity)) "
            + "from Invoice i join OrderItem oi on oi.order = i.order "
            + "where i.organizationId = :organizationId and oi.organizationId = :organizationId "
            + "and i.paidAt >= :from and i.paidAt < :toExclusive "
            + "group by oi.product.name "
            + "order by sum(oi.quantity) desc, oi.product.name asc")
    List<ProductQuantityResponse> sumItemsByProductInPeriod(@Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
}
