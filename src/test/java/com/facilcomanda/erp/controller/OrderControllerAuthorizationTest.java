package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.OrderService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static com.facilcomanda.erp.security.AuthTestSupport.assertAccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autorización por rol de /api/orders: crear/modificar comanda solo MESERO;
 * órdenes activas para MESERO/COCINERO/ADMIN/SUPERADMIN y, desde la feature 021
 * (roles.md decisión 5, STAFF_READ suma CAJERO), también CAJERO (las vistas de
 * gestión de caja listan órdenes); pendientes de pago solo CAJERO (criterio 3:
 * ADMIN recibe 403); cambio de estado solo COCINERO/ADMIN/SUPERADMIN.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class OrderControllerAuthorizationTest {

    private static final String VALID_ORDER_BODY =
            "{\"restaurantTableId\": 1, \"type\": \"DINE_IN\", \"idempotencyKey\": \"test-key-1\","
                    + " \"items\": [{\"productId\": 1, \"quantity\": 2}]}";

    private static final String VALID_STATUS_BODY = "{\"status\": \"PREPARING\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @ParameterizedTest
    @CsvSource({"MESERO,201", "COCINERO,403", "CAJERO,403", "ADMIN,403", "SUPERADMIN,403"})
    void createOrder(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/orders").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,403", "CAJERO,403", "ADMIN,403", "SUPERADMIN,403"})
    void updateOrder(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/orders/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,200", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getActiveOrders(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/orders").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,403", "SUPERADMIN,403"})
    void getPendingPaymentOrdersWithOccupiedTables(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/orders/pending-payment/occupied-tables").with(as(role))),
                expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,200", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void updateOrderStatus(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(patch("/api/orders/1/status").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_STATUS_BODY)), expectedStatus);
    }

    /**
     * Feature 027 (CA9) — marcar comanda atendida es exclusivo del MESERO
     * ({@code roles.md} decisión 6). Endpoint dedicado y sin cuerpo: no amplía
     * {@code KITCHEN_STATUS}, que seguiría permitiendo PREPARING/READY/CANCELLED.
     */
    @ParameterizedTest
    @CsvSource({"MESERO,200", "COCINERO,403", "CAJERO,403", "ADMIN,403", "SUPERADMIN,403"})
    void markOrderAttended(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(patch("/api/orders/1/attended").with(as(role))), expectedStatus);
    }
}
