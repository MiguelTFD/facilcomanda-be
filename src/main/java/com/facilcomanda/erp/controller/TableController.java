package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.TableRequest;
import com.facilcomanda.erp.dto.TableResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.TableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    @PreAuthorize(AuthorizationRules.IS_ADMIN)
    public ResponseEntity<TableResponse> createTable(@Valid @RequestBody TableRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return new ResponseEntity<>(tableService.createTable(request, orgId), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize(AuthorizationRules.STAFF_READ)
    public ResponseEntity<List<TableResponse>> getAllTables(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(tableService.getAllTables(orgId));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AuthorizationRules.STAFF_READ)
    public ResponseEntity<TableResponse> getTableById(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(tableService.getTableById(id, orgId));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AuthorizationRules.IS_ADMIN)
    public ResponseEntity<TableResponse> updateTable(@PathVariable Long id, @Valid @RequestBody TableRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(tableService.updateTable(id, request, orgId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AuthorizationRules.IS_ADMIN)
    public ResponseEntity<Void> deleteTable(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        tableService.deleteTable(id, orgId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/floor/{floorId}")
    @PreAuthorize(AuthorizationRules.STAFF_READ)
    public ResponseEntity<List<TableResponse>> getTablesByFloorId(@PathVariable Long floorId, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(tableService.getTablesByFloorAndOrganization(floorId, orgId));
    }
}
