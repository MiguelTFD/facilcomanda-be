package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.TableState;

public record TableResponse(
    Long id,
    String name,
    String description,
    TableState state,
    Integer chairs,
    Long organizationId,
    Long floorId,
    String floorName
) {}
