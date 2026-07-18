package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Expense> findByOrganizationIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
            Long organizationId, LocalDate from, LocalDate to);

    /** Total de egresos del período, agregado en base de datos (feature 022). */
    @Query("select coalesce(sum(e.amount), 0) from Expense e "
            + "where e.organizationId = :organizationId "
            + "and e.expenseDate between :from and :to")
    BigDecimal sumAmountInPeriod(@Param("organizationId") Long organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
