package com.facilcomanda.erp.security;

/**
 * Expresiones de autorización para {@code @PreAuthorize}, según la matriz de
 * roles de {@code spec/constitution/roles.md} (feature 001).
 */
public final class AuthorizationRules {

    /** Lectura operativa (mesas, productos, categorías, órdenes activas): personal que arma comandas. */
    public static final String STAFF_READ = "hasAnyRole('MESERO','COCINERO','ADMIN','SUPERADMIN')";

    /** Lectura de pisos: todos los roles, incluido CAJERO (la vista de caja carga los pisos). */
    public static final String FLOORS_READ = "hasAnyRole('MESERO','COCINERO','CAJERO','ADMIN','SUPERADMIN')";

    /** Gestión: CRUD de catálogo, pisos, mesas, usuarios, roles y organización propia. */
    public static final String IS_ADMIN = "hasAnyRole('ADMIN','SUPERADMIN')";

    /** Comandas: crear y modificar pedidos. */
    public static final String IS_MESERO = "hasRole('MESERO')";

    /** Caja: cobrar y consultar pendientes de pago. ADMIN queda excluido por decisión del propietario. */
    public static final String IS_CAJERO = "hasRole('CAJERO')";

    /** Cambio de estado de orden (cocina). */
    public static final String KITCHEN_STATUS = "hasAnyRole('COCINERO','ADMIN','SUPERADMIN')";

    /** Gestión entre organizaciones. */
    public static final String IS_SUPERADMIN = "hasRole('SUPERADMIN')";

    private AuthorizationRules() {
    }
}
