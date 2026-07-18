package com.facilcomanda.erp.exception;

/**
 * Excepción de dominio (feature 022) para violaciones de las reglas de un
 * egreso: monto ausente o no positivo, descripción vacía o fecha ausente. Se
 * mapea a HTTP 400 en {@code GlobalExceptionHandler}.
 */
public class ExpenseValidationException extends RuntimeException {

    public ExpenseValidationException(String message) {
        super(message);
    }
}
