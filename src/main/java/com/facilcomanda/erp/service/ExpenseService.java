package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ExpenseRequest;
import com.facilcomanda.erp.dto.ExpenseResponse;
import com.facilcomanda.erp.exception.ExpenseNotFoundException;
import com.facilcomanda.erp.exception.ExpenseValidationException;
import com.facilcomanda.erp.model.Expense;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.repository.ExpenseRepository;
import com.facilcomanda.erp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de egresos (feature 022). Las reglas de negocio se validan aquí con
 * excepciones de dominio y toda consulta va acotada al {@code organizationId}
 * del token (aislamiento multi-tenant).
 */
@Service
public class ExpenseService {

    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Long organizationId, String userEmail) {
        String description = validate(request);

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ExpenseValidationException("El usuario que registra el egreso no existe"));

        Expense expense = new Expense();
        expense.setOrganizationId(organizationId);
        expense.setAmount(request.amount());
        expense.setDescription(description);
        expense.setExpenseDate(request.expenseDate());
        expense.setRegisteredByUserId(author.getId());
        expense.setRegisteredByEmail(author.getEmail());
        expense.setCreatedAt(LocalDateTime.now());

        return mapToResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(Long organizationId, LocalDate from, LocalDate to) {
        return expenseRepository
                .findByOrganizationIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(organizationId, from, to)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long organizationId) {
        String description = validate(request);

        Expense expense = fetchExpense(id, organizationId);
        expense.setAmount(request.amount());
        expense.setDescription(description);
        expense.setExpenseDate(request.expenseDate());

        return mapToResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id, Long organizationId) {
        expenseRepository.delete(fetchExpense(id, organizationId));
    }

    private Expense fetchExpense(Long id, Long organizationId) {
        return expenseRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ExpenseNotFoundException("Egreso no encontrado"));
    }

    /**
     * Valida las reglas del egreso (decisión 2 de la spec 022) y devuelve la
     * descripción normalizada.
     */
    private String validate(ExpenseRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExpenseValidationException("El monto del egreso debe ser mayor a cero");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new ExpenseValidationException("La descripción del egreso es obligatoria");
        }
        String description = request.description().trim();
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ExpenseValidationException(
                    "La descripción del egreso no puede superar los " + DESCRIPTION_MAX_LENGTH + " caracteres");
        }
        if (request.expenseDate() == null) {
            throw new ExpenseValidationException("La fecha del egreso es obligatoria");
        }
        return description;
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getRegisteredByUserId(),
                expense.getRegisteredByEmail(),
                expense.getCreatedAt());
    }
}
