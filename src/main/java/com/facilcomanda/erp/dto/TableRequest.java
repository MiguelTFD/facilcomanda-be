package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.TableState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TableRequest(
    @NotBlank String name,
    String description,
    @NotNull TableState state,
    @NotNull Integer chairs,
    @NotNull Long floorId
) {}
