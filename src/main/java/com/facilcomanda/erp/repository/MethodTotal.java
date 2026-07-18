package com.facilcomanda.erp.repository;

import com.facilcomanda.erp.model.enums.PaymentMethod;
import java.math.BigDecimal;

/**
 * Proyección de la agregación por método de pago (feature 022). No forma parte
 * del contrato HTTP: {@code ReportService} la traduce al mapa
 * {@code collectedByMethod} de la respuesta.
 */
public record MethodTotal(PaymentMethod method, BigDecimal amount) {
}
