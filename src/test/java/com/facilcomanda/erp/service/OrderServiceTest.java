package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.OrderItemRequest;
import com.facilcomanda.erp.dto.OrderRequest;
import com.facilcomanda.erp.dto.OrderResponse;
import com.facilcomanda.erp.exception.OrderNotAttendableException;
import com.facilcomanda.erp.exception.OrderReductionNotAllowedException;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import com.facilcomanda.erp.model.Product;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.repository.OrderRepository;
import com.facilcomanda.erp.repository.ProductRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import com.facilcomanda.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 025 — pedidos por ronda. Fija el dominio de rondas sobre
 * {@link OrderService}: creación en ronda 1 con {@code createdAt} (CA1); edición
 * que agrega producto nuevo y aumenta uno existente → ronda 2 por delta, ronda 1
 * intacta, stock por delta, {@code modified} derivada (CA2); segunda edición →
 * ronda 3; reducción por debajo de lo enviado → excepción de dominio 4xx sin
 * efectos (CA3); campos de contrato y aislamiento multi-tenant (CA4).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long ORG_ID = 7L;
    private static final Long ORDER_ID = 42L;
    private static final String MESERO_EMAIL = "mesero@facilcomanda.test";

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RestaurantTableRepository restaurantTableRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private Product coca;   // id 1, precio 15
    private Product papas;  // id 2, precio 10

    @BeforeEach
    void setUp() {
        coca = product(1L, "Coca Cola", 10, "15.00");
        papas = product(2L, "Papas", 20, "10.00");
    }

    private Product product(Long id, String name, int stock, String price) {
        Product p = new Product(ORG_ID, name, null, stock, new BigDecimal(price), new BigDecimal("0.00"), null);
        p.setId(id);
        return p;
    }

    private OrderItem existingItem(Product p, int qty, int round, LocalDateTime createdAt) {
        OrderItem it = new OrderItem(ORG_ID, null, p, qty,
                p.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        it.setRoundNumber(round);
        it.setCreatedAt(createdAt);
        return it;
    }

    private Order pendingOrderWith(OrderItem... items) {
        Order order = new Order(ORG_ID, LocalDateTime.now().minusMinutes(30), "MESA", null,
                OrderStatus.PENDING, null, null, BigDecimal.ZERO);
        order.setId(ORDER_ID);
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem it : items) {
            order.addOrderItem(it);
            total = total.add(it.getSubtotal());
        }
        order.setTotal(total);
        return order;
    }

    // ---------- CA1: creación deja todo en ronda 1 con createdAt ----------

    @Test
    void createOrder_dejaTodosLosItemsEnRonda1ConCreatedAt_yModifiedFalse() {
        OrderRequest request = new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 2, null)));
        when(orderRepository.existsByIdempotencyKeyAndOrganizationId("idem-1", ORG_ID)).thenReturn(false);
        when(userRepository.findByEmail(MESERO_EMAIL)).thenReturn(Optional.of(new User()));
        when(productRepository.findByIdAndOrganizationId(1L, ORG_ID)).thenReturn(Optional.of(coca));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createOrder(request, ORG_ID, MESERO_EMAIL);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items()).allSatisfy(item -> {
            assertThat(item.roundNumber()).isEqualTo(1);
            assertThat(item.createdAt()).isNotNull();
        });
        assertThat(response.modified()).isFalse();
        assertThat(coca.getStock()).isEqualTo(8); // 10 - 2
    }

    // ---------- CA2: edición agrega ronda 2 por delta, ronda 1 intacta ----------

    @Test
    void updateOrder_agregaProductoNuevoYAumentaExistente_creaRonda2PorDeltaYPreservaRonda1() {
        LocalDateTime round1Time = LocalDateTime.now().minusMinutes(30);
        Order order = pendingOrderWith(existingItem(coca, 2, 1, round1Time));
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndOrganizationId(1L, ORG_ID)).thenReturn(Optional.of(coca));
        when(productRepository.findByIdAndOrganizationId(2L, ORG_ID)).thenReturn(Optional.of(papas));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // total acumulado por producto: coca 2->3 (+1), papas 0->3 (+3)
        OrderResponse response = orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 3, null), new OrderItemRequest(2L, 3, null))), ORG_ID);

        assertThat(order.getOrderItems()).hasSize(3);

        // ronda 1 intacta
        List<OrderItem> round1 = order.getOrderItems().stream().filter(i -> i.getRoundNumber() == 1).toList();
        assertThat(round1).hasSize(1);
        assertThat(round1.get(0).getProduct().getId()).isEqualTo(1L);
        assertThat(round1.get(0).getQuantity()).isEqualTo(2);
        assertThat(round1.get(0).getCreatedAt()).isEqualTo(round1Time);

        // ronda 2 = solo los deltas
        List<OrderItem> round2 = order.getOrderItems().stream().filter(i -> i.getRoundNumber() == 2).toList();
        assertThat(round2).hasSize(2);
        assertThat(round2).allSatisfy(i -> {
            assertThat(i.getCreatedAt()).isNotNull();
            assertThat(i.getOrganizationId()).isEqualTo(ORG_ID); // multi-tenant (CA4)
        });
        OrderItem cocaDelta = round2.stream().filter(i -> i.getProduct().getId() == 1L).findFirst().orElseThrow();
        OrderItem papasDelta = round2.stream().filter(i -> i.getProduct().getId() == 2L).findFirst().orElseThrow();
        assertThat(cocaDelta.getQuantity()).isEqualTo(1);
        assertThat(papasDelta.getQuantity()).isEqualTo(3);

        // stock descontado solo por el delta
        assertThat(coca.getStock()).isEqualTo(9);   // 10 - 1
        assertThat(papas.getStock()).isEqualTo(17); // 20 - 3

        // total = suma de todos los subtotales (todas las rondas)
        assertThat(response.total()).isEqualByComparingTo("75.00"); // 3*15 + 3*10
        assertThat(response.modified()).isTrue();

        // segunda edición -> ronda 3
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        OrderResponse second = orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 4, null), new OrderItemRequest(2L, 3, null))), ORG_ID);
        assertThat(order.getOrderItems().stream().anyMatch(i -> i.getRoundNumber() == 3)).isTrue();
        assertThat(second.items().stream().anyMatch(i -> i.roundNumber() == 3)).isTrue();
        assertThat(coca.getStock()).isEqualTo(8); // 9 - 1 (delta de la 2ª edición)
    }

    // ---------- CA3: reducir por debajo de lo enviado -> 4xx de dominio sin efectos ----------

    @Test
    void updateOrder_reduceCantidadDeUnProducto_lanzaExcepcionDeDominioSinEfectos() {
        Order order = pendingOrderWith(existingItem(coca, 2, 1, LocalDateTime.now().minusMinutes(10)));
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 1, null))), ORG_ID))
                .isInstanceOf(OrderReductionNotAllowedException.class);

        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(coca.getStock()).isEqualTo(10); // sin tocar stock
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrder_omiteUnProductoYaEnviado_lanzaExcepcionDeDominioSinEfectos() {
        Order order = pendingOrderWith(
                existingItem(coca, 2, 1, LocalDateTime.now().minusMinutes(10)),
                existingItem(papas, 1, 1, LocalDateTime.now().minusMinutes(10)));
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));

        // incoming omite papas (equivale a bajarlo a 0)
        assertThatThrownBy(() -> orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 2, null))), ORG_ID))
                .isInstanceOf(OrderReductionNotAllowedException.class);

        assertThat(order.getOrderItems()).hasSize(2);
        verify(orderRepository, never()).save(any());
    }

    // ---------- CA2 (borde): edición sin deltas no crea ronda ni marca modified ----------

    @Test
    void updateOrder_sinCambios_noCreaRondaNiMarcaModified() {
        Order order = pendingOrderWith(existingItem(coca, 2, 1, LocalDateTime.now().minusMinutes(10)));
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 2, null))), ORG_ID);

        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getRoundNumber()).isEqualTo(1);
        assertThat(response.modified()).isFalse();
        assertThat(coca.getStock()).isEqualTo(10);
    }

    // ================= Feature 027 — comanda atendida por el MESERO =================
    // CA2: marcar atendido; CA5/CA7: reactivación solo con delta; CA9: estados no
    // atendibles; CA10: aislamiento multi-tenant. Ver spec/features/027-*.

    private Order orderInStatus(OrderStatus status) {
        Order order = pendingOrderWith(existingItem(coca, 2, 1, LocalDateTime.now().minusMinutes(10)));
        order.setStatus(status);
        return order;
    }

    // ---------- CA2: marcar atendida ----------

    @Test
    void markAttended_poneStatusDeliveredYSellaAttendedAt() {
        Order order = orderInStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.markAttended(ORDER_ID, ORG_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getAttendedAt()).isNotNull();
        assertThat(response.attended()).isTrue();
        assertThat(response.attendedAt()).isEqualTo(order.getAttendedAt());
    }

    @Test
    void markAttended_ordenYaAtendida_esIdempotenteYNoMueveAttendedAt() {
        LocalDateTime sello = LocalDateTime.now().minusMinutes(5);
        Order order = orderInStatus(OrderStatus.DELIVERED);
        order.setAttendedAt(sello);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.markAttended(ORDER_ID, ORG_ID);

        assertThat(order.getAttendedAt()).isEqualTo(sello);
        assertThat(response.attended()).isTrue();
        verify(orderRepository, never()).save(any());
    }

    // ---------- CA9: estados no atendibles ----------

    @Test
    void markAttended_ordenPagada_lanzaExcepcionDeDominioSinEfectos() {
        Order order = orderInStatus(OrderStatus.PAID);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markAttended(ORDER_ID, ORG_ID))
                .isInstanceOf(OrderNotAttendableException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getAttendedAt()).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void markAttended_ordenCancelada_lanzaExcepcionDeDominioSinEfectos() {
        Order order = orderInStatus(OrderStatus.CANCELLED);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markAttended(ORDER_ID, ORG_ID))
                .isInstanceOf(OrderNotAttendableException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getAttendedAt()).isNull();
        verify(orderRepository, never()).save(any());
    }

    // ---------- CA10: aislamiento multi-tenant ----------

    @Test
    void markAttended_ordenDeOtraOrganizacion_noSeEncuentraNiSeModifica() {
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.markAttended(ORDER_ID, ORG_ID))
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any());
    }

    // ---------- CA5: una ronda nueva reactiva la comanda ----------

    @Test
    void updateOrder_conDeltaSobreOrdenAtendida_vuelveAPendingYConservaAttendedAt() {
        LocalDateTime sello = LocalDateTime.now().minusMinutes(5);
        Order order = orderInStatus(OrderStatus.DELIVERED);
        order.setAttendedAt(sello);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndOrganizationId(1L, ORG_ID)).thenReturn(Optional.of(coca));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 3, null))), ORG_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getAttendedAt()).isEqualTo(sello);
        assertThat(response.attended()).isFalse();
        assertThat(response.attendedAt()).isEqualTo(sello);
        assertThat(response.modified()).isTrue();
    }

    // ---------- CA7: editar sin agregar nada NO reactiva ----------

    @Test
    void updateOrder_sinDeltaSobreOrdenAtendida_siguePendienteDeCobroPeroAtendida() {
        LocalDateTime sello = LocalDateTime.now().minusMinutes(5);
        Order order = orderInStatus(OrderStatus.DELIVERED);
        order.setAttendedAt(sello);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 2, null))), ORG_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getAttendedAt()).isEqualTo(sello);
        assertThat(response.attended()).isTrue();
    }

    // ---------- contrato: attended derivada del estado ----------

    @Test
    void mapToResponse_ordenPendiente_exponeAttendedFalseYAttendedAtNulo() {
        Order order = orderInStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.updateOrder(ORDER_ID, new OrderRequest(null, "MESA", "idem-1",
                List.of(new OrderItemRequest(1L, 2, null))), ORG_ID);

        assertThat(response.attended()).isFalse();
        assertThat(response.attendedAt()).isNull();
    }
}
