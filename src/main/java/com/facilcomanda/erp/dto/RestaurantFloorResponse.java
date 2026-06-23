package com.facilcomanda.erp.dto;

public record RestaurantFloorResponse(
    Long id,
    String name,
    String description,
    Long organizationId
) {}
