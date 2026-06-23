package com.facilcomanda.erp.dto;

public record OrganizationResponse(
    Long id,
    String name,
    String taxIdentificationNumber,
    String taxIdentificationType
) {}
