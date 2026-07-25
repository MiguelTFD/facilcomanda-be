package com.facilcomanda.erp.security;

/**
 * Expresiones de autorización para {@code @PreAuthorize}, según la matriz de
 * roles de {@code spec/constitution/roles.md} (feature 001, y decisión 5 para
 * la feature 021).
 */
public final class AuthorizationRules {

    /**
     * Lectura operativa (mesas, productos, categorías, órdenes activas): personal
     * que arma comandas y, desde la feature 021 ({@code roles.md} decisión 5),
     * también CAJERO, que necesita estos listados para las vistas de gestión.
     */
    public static final String STAFF_READ = "hasAnyRole('MESERO','COCINERO','CAJERO','ADMIN','SUPERADMIN')";

    /** Lectura de pisos: todos los roles, incluido CAJERO (la vista de caja carga los pisos). */
    public static final String FLOORS_READ = "hasAnyRole('MESERO','COCINERO','CAJERO','ADMIN','SUPERADMIN')";

    /**
     * Gestión operativa: CRUD de catálogo, pisos, mesas y roles, e historial de
     * pagos/facturas. Desde la feature 021 ({@code roles.md} decisión 5) CAJERO
     * comparte estos accesos con ADMIN/SUPERADMIN. Excluye la gestión de usuarios
     * y de la organización, que siguen restringidas a {@link #IS_ADMIN}.
     */
    public static final String MANAGEMENT = "hasAnyRole('CAJERO','ADMIN','SUPERADMIN')";

    /** Gestión reservada a ADMIN: CRUD de usuarios y modificación de la organización propia. */
    public static final String IS_ADMIN = "hasAnyRole('ADMIN','SUPERADMIN')";

    /** Comandas: crear y modificar pedidos. */
    public static final String IS_MESERO = "hasRole('MESERO')";

    /** Caja: cobrar y consultar pendientes de pago. ADMIN queda excluido por decisión del propietario. */
    public static final String IS_CAJERO = "hasRole('CAJERO')";

    /** Cambio de estado de orden (cocina). */
    public static final String KITCHEN_STATUS = "hasAnyRole('COCINERO','ADMIN','SUPERADMIN')";

    /**
     * Marcar una comanda como atendida (feature 027, {@code roles.md} decisión 6).
     * Regla propia y endpoint dedicado en lugar de ampliar {@link #KITCHEN_STATUS}:
     * esa regla habilitaría además {@code PREPARING}, {@code READY} y
     * {@code CANCELLED}, mientras que aquí la única transición posible es a
     * {@code DELIVERED}.
     */
    public static final String ORDER_ATTEND = "hasRole('MESERO')";

    /** Gestión entre organizaciones. */
    public static final String IS_SUPERADMIN = "hasRole('SUPERADMIN')";

    private AuthorizationRules() {
    }
}
