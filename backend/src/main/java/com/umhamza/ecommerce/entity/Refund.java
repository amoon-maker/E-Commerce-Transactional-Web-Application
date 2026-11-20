package com.umhamza.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "REFUNDS", schema = "dbo")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id", nullable = false)
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Refund() {}

    public Refund(Payment payment, BigDecimal amount, String reason, String providerReference) {
        this.payment = payment;
        this.amount = amount;
        this.reason = reason;
        this.providerReference = providerReference;
        this.status = "COMPLETED";
        this.processedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getRefundId() { return refundId; }
    public Payment getPayment() { return payment; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
