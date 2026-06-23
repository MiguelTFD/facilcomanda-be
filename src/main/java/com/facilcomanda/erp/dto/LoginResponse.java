package com.facilcomanda.erp.dto;

public record LoginResponse(
    String token,
    UserAuthDTO user
) {}
