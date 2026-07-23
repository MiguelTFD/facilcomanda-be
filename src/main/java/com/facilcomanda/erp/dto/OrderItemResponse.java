package com.facilcomanda.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    Integer quantity,
    BigDecimal subtotal,
    String comments,
    LocalDateTime createdAt,
    Integer roundNumber
) {}
