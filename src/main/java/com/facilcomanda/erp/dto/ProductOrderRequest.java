package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductOrderRequest(@NotNull Long productId, @Min(1) int quantity) {
}
