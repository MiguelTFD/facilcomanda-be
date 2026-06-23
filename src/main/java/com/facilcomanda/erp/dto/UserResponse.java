package com.facilcomanda.erp.dto;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String roleName,
    Long organizationId
) {}
