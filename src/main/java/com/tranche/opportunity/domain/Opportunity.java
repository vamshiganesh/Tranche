package com.tranche.opportunity.domain;

import com.tranche.issuer.domain.Issuer;
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
import java.time.LocalDate;

@Entity
@Table(name = "opportunities")
@EntityListeners(AuditingEntityListener.class)
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuer_id", nullable = false)
    private Issuer issuer;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "face_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal faceValue;

    @Column(name = "discount_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "tenure_days", nullable = false)
    private Integer tenureDays;

    @Column(name = "minimum_lot", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumLot;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_grade", nullable = false, columnDefinition = "ENUM('A', 'B', 'C', 'D')")
    private RiskGrade riskGrade;

    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    @Column(name = "remaining_units", nullable = false)
    private Integer remainingUnits;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = """
            ENUM('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'LIVE', 'FULLY_SUBSCRIBED', 'MATURED', 'SETTLED')
            """)
    private OpportunityStatus status = OpportunityStatus.DRAFT;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "matured_at")
    private Instant maturedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

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

    public void setId(Long id) {
        this.id = id;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public Integer getTenureDays() {
        return tenureDays;
    }

    public void setTenureDays(Integer tenureDays) {
        this.tenureDays = tenureDays;
    }

    public BigDecimal getMinimumLot() {
        return minimumLot;
    }

    public void setMinimumLot(BigDecimal minimumLot) {
        this.minimumLot = minimumLot;
    }

    public RiskGrade getRiskGrade() {
        return riskGrade;
    }

    public void setRiskGrade(RiskGrade riskGrade) {
        this.riskGrade = riskGrade;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public Integer getRemainingUnits() {
        return remainingUnits;
    }

    public void setRemainingUnits(Integer remainingUnits) {
        this.remainingUnits = remainingUnits;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getMaturedAt() {
        return maturedAt;
    }

    public void setMaturedAt(Instant maturedAt) {
        this.maturedAt = maturedAt;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
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
