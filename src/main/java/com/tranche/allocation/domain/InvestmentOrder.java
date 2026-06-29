package com.tranche.allocation.domain;

import com.tranche.auth.domain.User;
import com.tranche.opportunity.domain.Opportunity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "investment_orders")
@EntityListeners(AuditingEntityListener.class)
public class InvestmentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private User investor;

    @Column(name = "idempotency_key", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String idempotencyKey;

    @Column(name = "units_requested", nullable = false)
    private Integer unitsRequested;

    @Column(name = "units_allocated", nullable = false)
    private Integer unitsAllocated = 0;

    @Column(name = "amount_requested", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountRequested;

    @Column(name = "amount_allocated", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountAllocated = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fill_status", nullable = false, columnDefinition = "ENUM('FULL', 'PARTIAL', 'REJECTED')")
    private FillStatus fillStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('PENDING', 'CONFIRMED', 'REJECTED')")
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "rejection_reason", length = 100)
    private String rejectionReason;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Long getId() {
        return id;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public User getInvestor() {
        return investor;
    }

    public void setInvestor(User investor) {
        this.investor = investor;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getUnitsRequested() {
        return unitsRequested;
    }

    public void setUnitsRequested(Integer unitsRequested) {
        this.unitsRequested = unitsRequested;
    }

    public Integer getUnitsAllocated() {
        return unitsAllocated;
    }

    public void setUnitsAllocated(Integer unitsAllocated) {
        this.unitsAllocated = unitsAllocated;
    }

    public BigDecimal getAmountRequested() {
        return amountRequested;
    }

    public void setAmountRequested(BigDecimal amountRequested) {
        this.amountRequested = amountRequested;
    }

    public BigDecimal getAmountAllocated() {
        return amountAllocated;
    }

    public void setAmountAllocated(BigDecimal amountAllocated) {
        this.amountAllocated = amountAllocated;
    }

    public FillStatus getFillStatus() {
        return fillStatus;
    }

    public void setFillStatus(FillStatus fillStatus) {
        this.fillStatus = fillStatus;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
