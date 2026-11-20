package com.umhamza.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "INVOICES", schema = "dbo")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "invoice_number", nullable = false, length = 40, unique = true)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Invoice() {}

    public Invoice(Order order, String invoiceNumber) {
        this.order = order;
        this.invoiceNumber = invoiceNumber;
        this.subtotal = order.getSubtotal();
        this.discountAmount = order.getDiscountAmount();
        this.taxAmount = order.getTaxAmount();
        this.shippingCost = order.getShippingCost();
        this.totalAmount = order.getTotalAmount();
        this.status = "ISSUED";
    }

    @PrePersist
    protected void onCreate() {
        if (issueDate == null) issueDate = LocalDate.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getInvoiceId() { return invoiceId; }
    public Order getOrder() { return order; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDate getIssueDate() { return issueDate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
