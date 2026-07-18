package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.InvoicePaymentRequest;
import com.facilcomanda.erp.dto.InvoiceResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.InvoiceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping("/orders/{orderId}/charge")
    @PreAuthorize(AuthorizationRules.IS_CAJERO)
    public ResponseEntity<InvoiceResponse> chargeOrder(@PathVariable Long orderId,
            @Valid @RequestBody InvoicePaymentRequest request,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        String email = authentication.getName();
        return new ResponseEntity<>(invoiceService.chargeOrder(orderId, request, orgId, email), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<List<InvoiceResponse>> getInvoices(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(invoiceService.getInvoices(orgId));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(invoiceService.getInvoiceById(id, orgId));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<InvoiceResponse> getInvoiceByOrderId(@PathVariable Long orderId,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(invoiceService.getInvoiceByOrderId(orderId, orgId));
    }
}
