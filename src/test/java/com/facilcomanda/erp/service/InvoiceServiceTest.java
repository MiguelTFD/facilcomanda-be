package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.InvoicePaymentRequest;
import com.facilcomanda.erp.dto.InvoiceResponse;
import com.facilcomanda.erp.dto.PaymentEntryRequest;
import com.facilcomanda.erp.exception.PaymentValidationException;
import com.facilcomanda.erp.model.Invoice;
import com.facilcomanda.erp.model.InvoicePayment;
import com.facilcomanda.erp.model.Order;
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
}
