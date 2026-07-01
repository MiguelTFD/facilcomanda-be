package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.InvoicePaymentRequest;
import com.facilcomanda.erp.dto.InvoiceResponse;
import com.facilcomanda.erp.model.Invoice;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.RestaurantTable;
import com.facilcomanda.erp.model.User;
import com.facilcomanda.erp.model.enums.OrderStatus;
import com.facilcomanda.erp.model.enums.TableState;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.OrderRepository;
import com.facilcomanda.erp.repository.RestaurantTableRepository;
import com.facilcomanda.erp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository,
            RestaurantTableRepository restaurantTableRepository, UserRepository userRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.userRepository = userRepository;
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
        if (request.amountPaid().compareTo(orderTotal) < 0) {
            throw new RuntimeException("Amount paid cannot be less than order total");
        }

        User cashier = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime paidAt = LocalDateTime.now();
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrder(order);
        invoice.setOrderNumber(order.getId());
        invoice.setInvoiceNumber(buildInvoiceNumber(organizationId, order.getId(), paidAt));
        invoice.setOrderTotal(orderTotal);
        invoice.setAmountPaid(request.amountPaid());
        invoice.setChangeAmount(request.amountPaid().subtract(orderTotal));
        invoice.setPaymentMethod(request.paymentMethod().trim());
        invoice.setPaidAt(paidAt);
        invoice.setCashier(cashier);
        invoice.setCashierEmail(cashier.getEmail());
        invoice.setNotes(request.notes());

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

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long organizationId) {
        return invoiceRepository.findByOrganizationIdOrderByPaidAtDesc(organizationId).stream()
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

    private String buildInvoiceNumber(Long organizationId, Long orderId, LocalDateTime paidAt) {
        return "INV-" + organizationId + "-" + orderId + "-" + paidAt.format(INVOICE_DATE_FORMAT);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
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
                invoice.getNotes());
    }
}
