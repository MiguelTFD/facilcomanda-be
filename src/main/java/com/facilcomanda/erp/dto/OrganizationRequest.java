package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(
    @NotBlank String name,
    String taxIdentificationNumber,
    String taxIdentificationType
) {}
