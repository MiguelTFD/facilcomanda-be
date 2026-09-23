package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.InvoiceItemResponse;
import com.facilcomanda.erp.dto.InvoicePaymentRequest;
import com.facilcomanda.erp.dto.InvoiceResponse;
import com.facilcomanda.erp.dto.PaymentEntryRequest;
import com.facilcomanda.erp.dto.PaymentEntryResponse;
import com.facilcomanda.erp.exception.PaymentValidationException;
import com.facilcomanda.erp.model.Invoice;
import com.facilcomanda.erp.model.InvoicePayment;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import com.facilcomanda.erp.model.RestaurantTable;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.model.enums.TableState;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.OrderRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import com.facilcomanda.erp.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final DateTimeFormatter INVOICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository,
            RestaurantTableRepository restaurantTableRepository, UserRepository userRepository, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceResponse chargeOrder(Long orderId, InvoicePaymentRequest request, Long organizationId, String userEmail) {
        Order order = orderRepository.findLockedByIdAndOrganizationId(orderId, organizationId)
                .orElseThrow(() -> new RuntimeException("Order not found or unauthorized"));

        if (invoiceRepository.existsByOrder_IdAndOrganizationId(orderId, organizationId)) {
            throw new RuntimeException("Order already has an invoice");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order is already paid");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cancelled orders cannot be charged");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Order must be pending payment before charging");
        }

        BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        PaymentTotals totals = validatePayments(request.payments(), orderTotal);

        User cashier = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrder(order);
        invoice.setOrderNumber(order.getId());
        invoice.setInvoiceNumber(buildInvoiceNumber(organizationId, order.getId(), paidAt));
        invoice.setOrderTotal(orderTotal);
        invoice.setAmountPaid(totals.sum());
        invoice.setChangeAmount(totals.sum().subtract(orderTotal));
        invoice.setPaymentMethod(totals.summaryMethod());
        invoice.setPaidAt(paidAt);
        invoice.setCashier(cashier);
        invoice.setCashierEmail(cashier.getEmail());
        invoice.setInvoiceType(request.invoiceType());
        invoice.setNotes(request.notes());

        for (PaymentEntryRequest entry : request.payments()) {
            InvoicePayment payment = new InvoicePayment();
            payment.setOrganizationId(organizationId);
            payment.setMethod(entry.method());
            payment.setAmount(entry.amount());
            payment.setReference(normalizeReference(entry.reference()));
            invoice.addPayment(payment);
        }

        RestaurantTable table = order.getRestaurantTable();
        if (table != null) {
            invoice.setRestaurantTableId(table.getId());
            invoice.setRestaurantTableName(table.getName());
            table.setState(TableState.AVAILABLE);
            restaurantTableRepository.save(table);
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToResponse(savedInvoice);
    }

    /**
     * Historial de pagos con las filas de pago y las líneas de producto ya
     * cargadas (feature 028). Son dos consultas de coste constante: sin ellas,
     * cada factura del listado dispararía las suyas propias (N+1).
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long organizationId) {
        List<Invoice> invoices = invoiceRepository.findAllWithPaymentsByOrganizationId(organizationId);
        if (invoices.isEmpty()) {
            return List.of();
        }
        invoiceRepository.fetchOrderItemsFor(invoices);
        return invoices.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id, Long organizationId) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new RuntimeException("Invoice not found or unauthorized"));
        return mapToResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId, Long organizationId) {
        Invoice invoice = invoiceRepository.findByOrder_IdAndOrganizationId(orderId, organizationId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order"));
        return mapToResponse(invoice);
    }

    /**
     * Valida las reglas de cobro multimétodo (feature 020) y devuelve la suma y
     * el resumen de método. Lanza {@link PaymentValidationException} (HTTP 400)
     * ante lista vacía, métodos duplicados, montos no positivos, suma menor al
     * total o excedente proveniente de un método distinto de EFECTIVO (el vuelto
     * solo puede salir del efectivo).
     */
    private PaymentTotals validatePayments(List<PaymentEntryRequest> payments, BigDecimal orderTotal) {
        if (payments == null || payments.isEmpty()) {
            throw new PaymentValidationException("At least one payment is required");
        }

        Set<PaymentMethod> seenMethods = EnumSet.noneOf(PaymentMethod.class);
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal nonCashSum = BigDecimal.ZERO;

        for (PaymentEntryRequest entry : payments) {
            if (entry.method() == null) {
                throw new PaymentValidationException("Payment method is required");
            }
            if (!seenMethods.add(entry.method())) {
                throw new PaymentValidationException("Duplicate payment method: " + entry.method());
            }
            if (entry.amount() == null || entry.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PaymentValidationException("Payment amount must be greater than zero");
            }
            sum = sum.add(entry.amount());
            if (entry.method() != PaymentMethod.EFECTIVO) {
                nonCashSum = nonCashSum.add(entry.amount());
            }
        }

        if (sum.compareTo(orderTotal) < 0) {
            throw new PaymentValidationException("Payments total cannot be less than order total");
        }
        if (nonCashSum.compareTo(orderTotal) > 0) {
            throw new PaymentValidationException("Non-cash payments cannot exceed order total");
        }

        String summaryMethod = seenMethods.size() == 1 ? seenMethods.iterator().next().name() : "MIXTO";
        return new PaymentTotals(sum, summaryMethod);
    }

    private String normalizeReference(String reference) {
        if (reference == null) {
            return null;
        }
        String trimmed = reference.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record PaymentTotals(BigDecimal sum, String summaryMethod) {
    }

    private String buildInvoiceNumber(Long organizationId, Long orderId, LocalDateTime paidAt) {
        return "INV-" + organizationId + "-" + orderId + "-" + paidAt.format(INVOICE_DATE_FORMAT);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<PaymentEntryResponse> payments = invoice.getPayments().stream()
                .map(payment -> new PaymentEntryResponse(
                        payment.getMethod(),
                        payment.getAmount(),
                        payment.getReference()))
                .collect(Collectors.toList());
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getOrder() != null ? invoice.getOrder().getId() : null,
                invoice.getOrderNumber(),
                invoice.getRestaurantTableId(),
                invoice.getRestaurantTableName(),
                invoice.getOrderTotal(),
                invoice.getAmountPaid(),
                invoice.getChangeAmount(),
                invoice.getPaymentMethod(),
                invoice.getPaidAt(),
                invoice.getCashier() != null ? invoice.getCashier().getId() : null,
                invoice.getCashierEmail(),
                invoice.getNotes(),
                payments,
                mapItems(invoice));
    }

    /**
     * Líneas del ticket de venta (feature 028), tomadas de la orden de la factura
     * y ordenadas por ronda de pedido y luego por id, que es el orden en que se
     * pidieron.
     */
    private List<InvoiceItemResponse> mapItems(Invoice invoice) {
        if (invoice.getOrder() == null || invoice.getOrder().getOrderItems() == null) {
            return List.of();
        }
        return invoice.getOrder().getOrderItems().stream()
                .sorted(Comparator
                        .comparing(OrderItem::getRoundNumber, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OrderItem::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(this::mapItem)
                .collect(Collectors.toList());
    }

    private InvoiceItemResponse mapItem(OrderItem item) {
        BigDecimal lineTotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        BigDecimal unitPrice = quantity > 0
                ? lineTotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String productName = item.getProduct() != null && item.getProduct().getName() != null
                ? item.getProduct().getName()
                : "(producto no disponible)";
        return new InvoiceItemResponse(productName, quantity, unitPrice, lineTotal, item.getComments());
    }
}
