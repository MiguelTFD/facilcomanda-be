package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.ReportSummaryResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes (feature 022). Consulta de gestión: CAJERO y ADMIN (regla
 * {@code MANAGEMENT}, decisión 5 de {@code spec/constitution/roles.md}).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @GetMapping("/summary")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<ReportSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(reportService.getSummary(orgId, from, to));
    }
}
