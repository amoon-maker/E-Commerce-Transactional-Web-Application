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

import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_STATUS_HISTORY", schema = "dbo")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedBy;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected OrderStatusHistory() {}

    public OrderStatusHistory(Order order, User changedBy, String status, String comment) {
        this.order = order;
        this.changedBy = changedBy;
        this.status = status;
        this.comment = comment;
    }

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }

    public Long getHistoryId() { return historyId; }
    public Order getOrder() { return order; }
    public User getChangedBy() { return changedBy; }
    public String getStatus() { return status; }
    public String getComment() { return comment; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
