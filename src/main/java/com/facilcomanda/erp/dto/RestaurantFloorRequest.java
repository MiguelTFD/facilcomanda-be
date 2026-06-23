package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;

public record RestaurantFloorRequest(
    @NotBlank String name,
    String description
) {}
