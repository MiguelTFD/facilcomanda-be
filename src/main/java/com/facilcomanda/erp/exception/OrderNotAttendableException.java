package com.facilcomanda.erp.exception;

/**
 * Se intentó marcar como atendida (feature 027) una orden que ya no admite esa
 * transición: órdenes {@code PAID} o {@code CANCELLED}. Excepción de dominio,
 * mapeada a 400 en {@code GlobalExceptionHandler}.
 */
public class OrderNotAttendableException extends RuntimeException {

    public OrderNotAttendableException(String message) {
        super(message);
    }
}
