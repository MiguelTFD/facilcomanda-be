package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.InvoiceItemResponse;
import com.facilcomanda.erp.dto.InvoicePaymentRequest;
import com.facilcomanda.erp.dto.InvoiceResponse;
import com.facilcomanda.erp.dto.PaymentEntryRequest;
import com.facilcomanda.erp.exception.PaymentValidationException;
import com.facilcomanda.erp.model.Invoice;
import com.facilcomanda.erp.model.InvoicePayment;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import com.facilcomanda.erp.model.Product;
import com.facilcomanda.erp.model.RestaurantTable;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.model.enums.TableState;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.OrderRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import com.facilcomanda.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 020 — cobro multimétodo. Fija CA1–CA4 y CA7 sobre
 * {@link InvoiceService#chargeOrder}: efectivo solo (comportamiento actual),
 * combinaciones (resumen MIXTO), crédito con referencia, rechazos 4xx por
 * regla de negocio y aislamiento multi-tenant de las filas de pago.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private static final Long ORG_ID = 7L;
    private static final Long ORDER_ID = 42L;
    private static final String CASHIER_EMAIL = "cajero@facilcomanda.test";

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RestaurantTableRepository restaurantTableRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    private Order order;
    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = new RestaurantTable(ORG_ID, "Mesa 3", null, TableState.OCCUPIED, 4);
        table.setId(9L);

        order = new Order(ORG_ID, null, "MESA", null, OrderStatus.DELIVERED, table, null, new BigDecimal("52.00"));
        order.setId(ORDER_ID);
    }

    private void stubHappyPathDependencies() {
        when(orderRepository.findLockedByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByOrder_IdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(false);
        User cashier = new User();
        cashier.setId(3L);
        cashier.setEmail(CASHIER_EMAIL);
        when(userRepository.findByEmail(CASHIER_EMAIL)).thenReturn(Optional.of(cashier));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubValidationDependencies() {
        when(orderRepository.findLockedByIdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByOrder_IdAndOrganizationId(ORDER_ID, ORG_ID)).thenReturn(false);
    }

    private InvoiceResponse charge(InvoicePaymentRequest request) {
        return invoiceService.chargeOrder(ORDER_ID, request, ORG_ID, CASHIER_EMAIL);
    }

    // ---------- CA1: solo efectivo (comportamiento actual) ----------

    @Test
    void chargeOrder_soloEfectivoMontoExacto_creaFacturaConUnPagoYCierraLaOrden() {
        order.setTotal(new BigDecimal("52.00"));
        stubHappyPathDependencies();

        InvoiceResponse response = charge(new InvoicePaymentRequest(
                List.of(new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("52.00"), null)), null));

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice saved = invoiceCaptor.getValue();
        assertThat(saved.getAmountPaid()).isEqualByComparingTo("52.00");
        assertThat(saved.getChangeAmount()).isEqualByComparingTo("0.00");
        assertThat(saved.getPaymentMethod()).isEqualTo("EFECTIVO");
        assertThat(saved.getPayments()).hasSize(1);
        assertThat(saved.getPayments().get(0).getMethod()).isEqualTo(PaymentMethod.EFECTIVO);
        assertThat(saved.getPayments().get(0).getAmount()).isEqualByComparingTo("52.00");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(table.getState()).isEqualTo(TableState.AVAILABLE);
        verify(restaurantTableRepository).save(table);

        assertThat(response.paymentMethod()).isEqualTo("EFECTIVO");
        assertThat(response.payments()).hasSize(1);
    }

    @Test
    void chargeOrder_soloEfectivoConExcedente_calculaVuelto() {
        order.setTotal(new BigDecimal("50.00"));
        stubHappyPathDependencies();

        charge(new InvoicePaymentRequest(
                List.of(new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("60.00"), null)), null));

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice saved = invoiceCaptor.getValue();
        assertThat(saved.getAmountPaid()).isEqualByComparingTo("60.00");
        assertThat(saved.getChangeAmount()).isEqualByComparingTo("10.00");
        assertThat(saved.getPaymentMethod()).isEqualTo("EFECTIVO");
    }

    // ---------- CA2: combinado -> MIXTO ----------

    @Test
    void chargeOrder_yapeMasEfectivo_resumenMixtoSinVuelto() {
        order.setTotal(new BigDecimal("52.00"));
        stubHappyPathDependencies();

        charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.YAPE, new BigDecimal("30.00"), null),
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("22.00"), null)), null));

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice saved = invoiceCaptor.getValue();
        assertThat(saved.getPaymentMethod()).isEqualTo("MIXTO");
        assertThat(saved.getAmountPaid()).isEqualByComparingTo("52.00");
        assertThat(saved.getChangeAmount()).isEqualByComparingTo("0.00");
        assertThat(saved.getPayments()).hasSize(2);
    }

    // ---------- CA3: crédito con referencia ----------

    @Test
    void chargeOrder_efectivoMasCredito_cierraLaOrdenYGuardaReferencia() {
        order.setTotal(new BigDecimal("52.00"));
        stubHappyPathDependencies();

        charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("20.00"), null),
                new PaymentEntryRequest(PaymentMethod.CREDITO, new BigDecimal("32.00"), "Sr. Pérez")), null));

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice saved = invoiceCaptor.getValue();
        assertThat(saved.getPaymentMethod()).isEqualTo("MIXTO");
        assertThat(saved.getChangeAmount()).isEqualByComparingTo("0.00");
        InvoicePayment credito = saved.getPayments().stream()
                .filter(p -> p.getMethod() == PaymentMethod.CREDITO).findFirst().orElseThrow();
        assertThat(credito.getAmount()).isEqualByComparingTo("32.00");
        assertThat(credito.getReference()).isEqualTo("Sr. Pérez");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(table.getState()).isEqualTo(TableState.AVAILABLE);
    }

    // ---------- CA7: aislamiento multi-tenant ----------

    @Test
    void chargeOrder_asignaOrganizationIdALaFacturaYACadaPago() {
        order.setTotal(new BigDecimal("52.00"));
        stubHappyPathDependencies();

        charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.YAPE, new BigDecimal("30.00"), null),
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("22.00"), null)), null));

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice saved = invoiceCaptor.getValue();
        assertThat(saved.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(saved.getPayments()).allSatisfy(p -> {
            assertThat(p.getOrganizationId()).isEqualTo(ORG_ID);
            assertThat(p.getInvoice()).isSameAs(saved);
        });
    }

    // ---------- CA4: rechazos 4xx (excepción de dominio) ----------

    @Test
    void chargeOrder_listaVacia_lanzaPaymentValidationException() {
        order.setTotal(new BigDecimal("52.00"));
        stubValidationDependencies();

        assertThatThrownBy(() -> charge(new InvoicePaymentRequest(List.of(), null)))
                .isInstanceOf(PaymentValidationException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void chargeOrder_metodosDuplicados_lanzaPaymentValidationException() {
        order.setTotal(new BigDecimal("52.00"));
        stubValidationDependencies();

        assertThatThrownBy(() -> charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("30.00"), null),
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("22.00"), null)), null)))
                .isInstanceOf(PaymentValidationException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void chargeOrder_montoNoPositivo_lanzaPaymentValidationException() {
        order.setTotal(new BigDecimal("52.00"));
        stubValidationDependencies();

        assertThatThrownBy(() -> charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("52.00"), null),
                new PaymentEntryRequest(PaymentMethod.YAPE, BigDecimal.ZERO, null)), null)))
                .isInstanceOf(PaymentValidationException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void chargeOrder_sumaMenorAlTotal_lanzaPaymentValidationException() {
        order.setTotal(new BigDecimal("52.00"));
        stubValidationDependencies();

        assertThatThrownBy(() -> charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.EFECTIVO, new BigDecimal("50.00"), null)), null)))
                .isInstanceOf(PaymentValidationException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void chargeOrder_noEfectivoExcedeElTotal_lanzaPaymentValidationException() {
        order.setTotal(new BigDecimal("52.00"));
        stubValidationDependencies();

        assertThatThrownBy(() -> charge(new InvoicePaymentRequest(List.of(
                new PaymentEntryRequest(PaymentMethod.YAPE, new BigDecimal("60.00"), null)), null)))
                .isInstanceOf(PaymentValidationException.class);
        verify(invoiceRepository, never()).save(any());
    }

    // ---------- Feature 028: líneas de producto en el ticket ----------

    private OrderItem itemOf(Long id, String productName, int quantity, String subtotal, int round, String comments) {
        Product product = new Product();
        product.setId(id);
        product.setName(productName);

        OrderItem item = new OrderItem(ORG_ID, order, product, quantity, new BigDecimal(subtotal));
        item.setId(id);
        item.setRoundNumber(round);
        item.setComments(comments);
        return item;
    }

    private Invoice invoiceWithItems(List<OrderItem> items) {
        order.getOrderItems().clear();
        order.getOrderItems().addAll(items);

        Invoice invoice = new Invoice();
        invoice.setId(100L);
        invoice.setOrganizationId(ORG_ID);
        invoice.setOrder(order);
        invoice.setOrderNumber(ORDER_ID);
        invoice.setInvoiceNumber("INV-7-42-20260801143205");
        invoice.setOrderTotal(new BigDecimal("55.00"));
        invoice.setAmountPaid(new BigDecimal("60.00"));
        invoice.setChangeAmount(new BigDecimal("5.00"));
        invoice.setPaymentMethod("EFECTIVO");
        invoice.setPaidAt(LocalDateTime.of(2026, 8, 1, 14, 32));
        return invoice;
    }

    @Test
    void getInvoiceById_mapeaLineasConPrecioUnitarioDerivado() {
        Invoice invoice = invoiceWithItems(List.of(
                itemOf(1L, "Lomo saltado", 2, "50.00", 1, null),
                itemOf(2L, "Chicha morada", 1, "5.00", 1, "sin hielo")));
        when(invoiceRepository.findByIdAndOrganizationId(100L, ORG_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(100L, ORG_ID);

        assertThat(response.items()).hasSize(2);
        InvoiceItemResponse first = response.items().get(0);
        assertThat(first.productName()).isEqualTo("Lomo saltado");
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.unitPrice()).isEqualByComparingTo("25.00");
        assertThat(first.lineTotal()).isEqualByComparingTo("50.00");
        assertThat(first.comments()).isNull();
        assertThat(response.items().get(1).comments()).isEqualTo("sin hielo");
    }

    @Test
    void getInvoiceById_ordenaLineasPorRondaYLuegoPorId() {
        Invoice invoice = invoiceWithItems(List.of(
                itemOf(9L, "Postre", 1, "10.00", 2, null),
                itemOf(3L, "Entrada", 1, "8.00", 1, null),
                itemOf(7L, "Segundo", 1, "20.00", 1, null)));
        when(invoiceRepository.findByIdAndOrganizationId(100L, ORG_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(100L, ORG_ID);

        assertThat(response.items())
                .extracting(InvoiceItemResponse::productName)
                .containsExactly("Entrada", "Segundo", "Postre");
    }

    @Test
    void getInvoiceById_sumaDeLineasCoincideConElTotalDeLaFactura() {
        Invoice invoice = invoiceWithItems(List.of(
                itemOf(1L, "Lomo saltado", 2, "50.00", 1, null),
                itemOf(2L, "Chicha morada", 1, "5.00", 1, null)));
        when(invoiceRepository.findByIdAndOrganizationId(100L, ORG_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(100L, ORG_ID);

        BigDecimal suma = response.items().stream()
                .map(InvoiceItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(response.orderTotal());
    }

    @Test
    void getInvoiceById_divisionNoExacta_redondeaUnitarioYRespetaElSubtotal() {
        Invoice invoice = invoiceWithItems(List.of(itemOf(1L, "Menu del dia", 3, "10.00", 1, null)));
        when(invoiceRepository.findByIdAndOrganizationId(100L, ORG_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(100L, ORG_ID);

        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("3.33");
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void getInvoiceById_facturaSinLineas_devuelveListaVaciaNoNula() {
        Invoice invoice = invoiceWithItems(List.of());
        when(invoiceRepository.findByIdAndOrganizationId(100L, ORG_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(100L, ORG_ID);

        assertThat(response.items()).isNotNull().isEmpty();
    }

    @Test
    void getInvoiceById_productoBorrado_noRompeElTicket() {
        OrderItem huerfano = new OrderItem(ORG_ID, order, null, 1, new BigDecimal("12.00"));
        huerfano.setId(5L);
        huerfano.setRoundNumber(1);
        Invoice invoice = invoiceWithItems(List.of(huerfano));
        when(invoiceRepository.findByIdAndOrganizationId(100L, ORG_ID)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById(100L, ORG_ID);

        assertThat(response.items().get(0).productName()).isEqualTo("(producto no disponible)");
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("12.00");
    }

    // ---------- Feature 028: carga sin N+1 y aislamiento multi-tenant (CA8, CA9) ----------

    @Test
    void getInvoices_usaLasConsultasConFetchYNoLaConsultaSimple() {
        Invoice invoice = invoiceWithItems(List.of(itemOf(1L, "Lomo saltado", 2, "50.00", 1, null)));
        when(invoiceRepository.findAllWithPaymentsByOrganizationId(ORG_ID)).thenReturn(List.of(invoice));

        List<InvoiceResponse> responses = invoiceService.getInvoices(ORG_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).items()).hasSize(1);
        verify(invoiceRepository).fetchOrderItemsFor(List.of(invoice));
        verify(invoiceRepository, never()).findByOrganizationIdOrderByPaidAtDesc(ORG_ID);
    }

    @Test
    void getInvoices_sinFacturas_noLanzaLaSegundaConsulta() {
        when(invoiceRepository.findAllWithPaymentsByOrganizationId(ORG_ID)).thenReturn(List.of());

        assertThat(invoiceService.getInvoices(ORG_ID)).isEmpty();

        verify(invoiceRepository, never()).fetchOrderItemsFor(any());
    }

    @Test
    void getInvoices_filtraSiemprePorLaOrganizacionDelToken() {
        Long otraOrg = 99L;
        when(invoiceRepository.findAllWithPaymentsByOrganizationId(otraOrg)).thenReturn(List.of());

        invoiceService.getInvoices(otraOrg);

        verify(invoiceRepository).findAllWithPaymentsByOrganizationId(otraOrg);
        verify(invoiceRepository, never()).findAllWithPaymentsByOrganizationId(ORG_ID);
    }

    @Test
    void getInvoiceById_deOtraOrganizacion_noDevuelveLineas() {
        when(invoiceRepository.findByIdAndOrganizationId(100L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(100L, 99L))
                .isInstanceOf(RuntimeException.class);

        verify(invoiceRepository, never()).findByIdAndOrganizationId(100L, ORG_ID);
    }
}
