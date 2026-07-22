package com.facilcomanda.erp.exception;

/**
 * Excepción de dominio (feature 025, decisión D3): al modificar una comanda no se
 * permite bajar la cantidad total de un producto por debajo de lo ya enviado a
 * cocina (ni omitir un producto ya enviado). Lo ya pedido no se "des-pide" desde
 * este flujo. Se mapea a HTTP 400 en {@code GlobalExceptionHandler}.
 */
public class OrderReductionNotAllowedException extends RuntimeException {

    public OrderReductionNotAllowedException(String message) {
        super(message);
    }
}
