package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    Long restaurantTableId,
    String tableName,
    String floorName,
    String type,
    OrderStatus status,
    BigDecimal total,
    LocalDateTime orderDate,
    List<OrderItemResponse> items,
    Boolean modified
) {}
