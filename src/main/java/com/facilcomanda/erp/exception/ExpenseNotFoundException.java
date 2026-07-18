package com.facilcomanda.erp.exception;

/**
 * Excepción de dominio (feature 022) para un egreso inexistente o ajeno a la
 * organización del token. Se mapea a HTTP 404 en
 * {@code GlobalExceptionHandler}; nunca revela si el egreso existe en otra
 * organización.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
