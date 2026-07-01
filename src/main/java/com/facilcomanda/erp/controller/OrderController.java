package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.OrderRequest;
import com.facilcomanda.erp.dto.OrderResponse;
import com.facilcomanda.erp.dto.OrderStatusRequest;
import com.facilcomanda.erp.security.CustomAuthentication;
import com.facilcomanda.erp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private Long getOrganizationId(Authentication authentication) {
        if (authentication instanceof CustomAuthentication customAuth) {
            return customAuth.getOrganizationId();
        }
        throw new RuntimeException("Authentication is invalid or missing organization ID context");
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        String email = authentication.getName();
        return new ResponseEntity<>(orderService.createOrder(request, orgId, email), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getActiveOrders(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(orderService.getActiveOrders(orgId));
    }

    @GetMapping("/pending-payment/occupied-tables")
    public ResponseEntity<List<OrderResponse>> getPendingPaymentOrdersWithOccupiedTables(Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(orderService.getPendingPaymentOrdersWithOccupiedTables(orgId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request, orgId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id,
            @Valid @RequestBody OrderRequest request,
            Authentication authentication) {
        Long orgId = getOrganizationId(authentication);
        return ResponseEntity.ok(orderService.updateOrder(id, request, orgId));
    }
}
