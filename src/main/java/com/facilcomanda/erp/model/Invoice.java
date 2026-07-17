package com.facilcomanda.erp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "invoices",
        indexes = {
                @Index(name = "idx_invoice_org", columnList = "organization_id"),
                @Index(name = "idx_invoice_order", columnList = "order_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_invoice_order", columnNames = "order_id"))
@Filter(name = "tenantFilter", condition = "organization_id = :organizationId")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_number", nullable = false)
    private Long orderNumber;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 80)
    private String invoiceNumber;

    @Column(name = "order_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal orderTotal;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "change_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_user_id")
    private User cashier;

    @Column(name = "cashier_email")
    private String cashierEmail;

    @Column(name = "restaurant_table_id")
    private Long restaurantTableId;

    @Column(name = "restaurant_table_name")
    private String restaurantTableName;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoicePayment> payments = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public User getCashier() {
        return cashier;
    }

    public void setCashier(User cashier) {
        this.cashier = cashier;
    }

    public String getCashierEmail() {
        return cashierEmail;
    }

    public void setCashierEmail(String cashierEmail) {
        this.cashierEmail = cashierEmail;
    }

    public Long getRestaurantTableId() {
        return restaurantTableId;
    }

    public void setRestaurantTableId(Long restaurantTableId) {
        this.restaurantTableId = restaurantTableId;
    }

    public String getRestaurantTableName() {
        return restaurantTableName;
    }

    public void setRestaurantTableName(String restaurantTableName) {
        this.restaurantTableName = restaurantTableName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<InvoicePayment> getPayments() {
        return payments;
    }

    public void setPayments(List<InvoicePayment> payments) {
        this.payments = payments;
    }

    public void addPayment(InvoicePayment payment) {
        payment.setInvoice(this);
        this.payments.add(payment);
    }
}
