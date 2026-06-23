package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.RoleName;

public record UserAuthDTO(
    Long id,
    String email,
    String firstName,
    String lastName,
    RoleName roleName,
    Long organizationId
) {}
