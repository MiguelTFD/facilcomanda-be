package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
    @NotBlank String name,
    String description,
    Long parentCategoryId
) {}
