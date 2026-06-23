package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(
        Long restaurantTableId,
        @NotBlank String type,
        @NotBlank String idempotencyKey,
        @NotEmpty List<OrderItemRequest> items) {
}
