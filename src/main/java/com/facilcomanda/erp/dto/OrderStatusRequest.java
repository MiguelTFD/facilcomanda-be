package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}
