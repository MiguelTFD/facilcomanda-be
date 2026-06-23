package com.facilcomanda.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    String description,
    Integer stock,
    BigDecimal unitPrice,
    BigDecimal discount,
    LocalDateTime expirationDate,
    List<Long> categoryIds,
    List<String> categoryNames
) {}
