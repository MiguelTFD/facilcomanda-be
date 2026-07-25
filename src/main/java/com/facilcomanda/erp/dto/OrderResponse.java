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
    Boolean modified,
    /** Feature 027: la comanda ya fue marcada como atendida por el MESERO ({@code status == DELIVERED}). */
    Boolean attended,
    /** Feature 027: momento del último atendido; nulo si nunca se atendió. Referencia para detectar rondas nuevas en cocina. */
    LocalDateTime attendedAt
) {}
