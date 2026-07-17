package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.RestaurantFloorRequest;
import com.facilcomanda.erp.dto.RestaurantFloorResponse;
import com.facilcomanda.erp.security.AuthorizationRules;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.RestaurantFloorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant-floors")
public class RestaurantFloorController {

    private final RestaurantFloorService restaurantFloorService;

    public RestaurantFloorController(RestaurantFloorService restaurantFloorService) {
        this.restaurantFloorService = restaurantFloorService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<RestaurantFloorResponse> createFloor(@Valid @RequestBody RestaurantFloorRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return new ResponseEntity<>(restaurantFloorService.createFloor(request, orgId), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize(AuthorizationRules.FLOORS_READ)
    public ResponseEntity<List<RestaurantFloorResponse>> getAllFloors(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(restaurantFloorService.getAllFloors(orgId));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AuthorizationRules.FLOORS_READ)
    public ResponseEntity<RestaurantFloorResponse> getFloorById(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(restaurantFloorService.getFloorById(id, orgId));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<RestaurantFloorResponse> updateFloor(@PathVariable Long id, @Valid @RequestBody RestaurantFloorRequest request, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(restaurantFloorService.updateFloor(id, request, orgId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AuthorizationRules.MANAGEMENT)
    public ResponseEntity<Void> deleteFloor(@PathVariable Long id, Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        restaurantFloorService.deleteFloor(id, orgId);
        return ResponseEntity.noContent().build();
    }
}
