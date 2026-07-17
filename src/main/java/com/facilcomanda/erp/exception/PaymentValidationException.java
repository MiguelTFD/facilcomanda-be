package com.facilcomanda.erp.exception;

/**
 * Excepción de dominio (feature 020) para violaciones de las reglas de cobro
 * multimétodo: lista de pagos vacía, métodos duplicados, montos no positivos,
 * suma menor al total o excedente proveniente de un método distinto de
 * EFECTIVO. Se mapea a HTTP 400 en {@code GlobalExceptionHandler}.
 */
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }
}
