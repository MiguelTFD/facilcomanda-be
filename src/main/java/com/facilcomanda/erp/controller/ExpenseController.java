package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.ExpenseRequest;
import com.facilcomanda.erp.dto.ExpenseResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.ExpenseService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Egresos (feature 022). Los registran y corrigen CAJERO y ADMIN (regla
 * {@code MANAGEMENT}, decisión 5 de {@code spec/constitution/roles.md}).
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<ExpenseResponse> createExpense(@RequestBody ExpenseRequest request,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        String email = authentication.getName();
        return new ResponseEntity<>(expenseService.createExpense(request, orgId, email), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(expenseService.getExpenses(orgId, from, to));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
            @RequestBody ExpenseRequest request,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(expenseService.updateExpense(id, request, orgId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        expenseService.deleteExpense(id, orgId);
        return ResponseEntity.noContent().build();
    }
}
