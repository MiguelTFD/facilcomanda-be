package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.OrganizationRequest;
import com.facilcomanda.erp.dto.OrganizationResponse;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(roleName) || role.equals("ROLE_" + roleName));
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationRequest request, Authentication authentication) {
        if (!hasRole(authentication, "WEB_OWNER")) {
            throw new RuntimeException("Unauthorized: Only WEB_OWNER can create organizations");
        }
        return new ResponseEntity<>(organizationService.createOrganization(request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<OrganizationResponse> getMyOrganization(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(organizationService.getOrganizationById(orgId));
    }

    @PutMapping("/my")
    public ResponseEntity<OrganizationResponse> updateMyOrganization(@Valid @RequestBody OrganizationRequest request, Authentication authentication) {
        if (!hasRole(authentication, "ORG_MASTER")) {
            throw new RuntimeException("Unauthorized: Only ORG_MASTER can update this organization");
        }
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(organizationService.updateOrganization(orgId, request));
    }
}
