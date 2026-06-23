package com.facilcomanda.erp.dto;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    Long parentCategoryId
) {}
