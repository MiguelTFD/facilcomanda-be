package com.facilcomanda.erp.dto;

/** Desglose de "cantidad de platos" por producto (feature 022). */
public record ProductQuantityResponse(
        String productName,
        Long quantity) {
}
