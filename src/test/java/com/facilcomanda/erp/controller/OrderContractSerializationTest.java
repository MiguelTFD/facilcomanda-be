package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.OrderItemResponse;
import com.facilcomanda.erp.dto.OrderResponse;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature 025 (CA4) — contrato de serialización de /api/orders: cada ítem expone
 * {@code createdAt} y {@code roundNumber}, y la orden expone {@code modified}
 * derivada de los datos. Fija la forma de respuesta en el cable.
 */
@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class OrderContractSerializationTest {

    private static final String VALID_ORDER_BODY =
            "{\"restaurantTableId\": 1, \"type\": \"MESA\", \"idempotencyKey\": \"k1\","
                    + " \"items\": [{\"productId\": 1, \"quantity\": 2}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_respuestaIncluyeCreatedAtRoundNumberYModified() throws Exception {
        OrderItemResponse round1 = new OrderItemResponse(10L, 1L, "Coca Cola", 2,
                new BigDecimal("30.00"), null, LocalDateTime.parse("2026-07-01T12:00:00"), 1);
        OrderItemResponse round2 = new OrderItemResponse(11L, 2L, "Papas", 3,
                new BigDecimal("30.00"), null, LocalDateTime.parse("2026-07-01T12:20:00"), 2);
        OrderResponse response = new OrderResponse(100L, 1L, "Mesa 1", "Piso 1", "MESA",
                OrderStatus.PENDING, new BigDecimal("60.00"), LocalDateTime.parse("2026-07-01T12:00:00"),
                List.of(round1, round2), true);
        when(orderService.createOrder(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/orders").with(as(RoleName.MESERO))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modified").value(true))
                .andExpect(jsonPath("$.items[0].roundNumber").value(1))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[1].roundNumber").value(2))
                .andExpect(jsonPath("$.items[1].createdAt").exists());
    }
}
