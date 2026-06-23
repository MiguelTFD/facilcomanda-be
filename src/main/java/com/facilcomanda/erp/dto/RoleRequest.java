package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(
    @NotBlank String name,
    String description
) {}
