package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.RoleRequest;
import com.facilcomanda.erp.dto.RoleResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize(AuthorizationRules.MANAGEMENT)
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request, Authentication authentication) {
        return new ResponseEntity<>(roleService.createRole(request, getOrganizationId(authentication)), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles(Authentication authentication) {
        return ResponseEntity.ok(roleService.getAllRoles(getOrganizationId(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(roleService.getRoleById(id, getOrganizationId(authentication)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request, Authentication authentication) {
        return ResponseEntity.ok(roleService.updateRole(id, request, getOrganizationId(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id, Authentication authentication) {
        roleService.deleteRole(id, getOrganizationId(authentication));
        return ResponseEntity.noContent().build();
    }
}
