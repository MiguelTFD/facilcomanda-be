package com.facilcomanda.erp.model.enums;

/**
 * Franjas del día en el reporte diario (feature 034). No se persiste: es un
 * concepto de reporte, no de dominio. Las fronteras son fijas y viven en
 * {@code ReportService} (decisión 2 de la spec).
 */
public enum DayShift {
    TARDE,
    NOCHE,
    OTROS
}
