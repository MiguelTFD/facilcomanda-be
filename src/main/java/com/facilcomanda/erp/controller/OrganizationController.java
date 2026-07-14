package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.OrganizationRequest;
import com.facilcomanda.erp.dto.OrganizationResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    @PreAuthorize(AuthorizationRules.IS_SUPERADMIN)
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationRequest request, Authentication authentication) {
        return new ResponseEntity<>(organizationService.createOrganization(request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<OrganizationResponse> getMyOrganization(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(organizationService.getOrganizationById(orgId));
    }

    @PutMapping("/my")
    @PreAuthorize(AuthorizationRules.IS_ADMIN)
    public ResponseEntity<OrganizationResponse> updateMyOrganization(@Valid @RequestBody OrganizationRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, request));
    }
}
