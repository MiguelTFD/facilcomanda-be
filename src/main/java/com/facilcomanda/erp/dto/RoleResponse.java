package com.facilcomanda.erp.dto;

public record RoleResponse(
    Long id,
    String name,
    String description,
    Long organizationId
) {}
