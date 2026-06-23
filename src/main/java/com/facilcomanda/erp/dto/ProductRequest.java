package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotNull Integer stock,
    @NotNull BigDecimal unitPrice,
    BigDecimal discount,
    LocalDateTime expirationDate,
    Set<Long> categoryIds
) {}
