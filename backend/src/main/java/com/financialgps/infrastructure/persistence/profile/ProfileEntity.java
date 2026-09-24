package com.financialgps.infrastructure.persistence.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Profile row: scalar facts only. Derived totals are recomputed, never stored. */
@Entity
@Table(name = "profile")
public class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "savings_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal savingsAmount = BigDecimal.ZERO;

    @Column(name = "emergency_fund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal emergencyFundAmount = BigDecimal.ZERO;

    @Column(name = "dependents_count", nullable = false)
    private int dependentsCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ProfileEntity() {
    }

    public ProfileEntity(UUID ownerId, String currency, BigDecimal savingsAmount,
                         BigDecimal emergencyFundAmount, int dependentsCount) {
        this.ownerId = ownerId;
        this.currency = currency;
        this.savingsAmount = savingsAmount;
        this.emergencyFundAmount = emergencyFundAmount;
        this.dependentsCount = dependentsCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSavingsAmount() {
        return savingsAmount;
    }

    public void setSavingsAmount(BigDecimal savingsAmount) {
        this.savingsAmount = savingsAmount;
    }

    public BigDecimal getEmergencyFundAmount() {
        return emergencyFundAmount;
    }

    public void setEmergencyFundAmount(BigDecimal emergencyFundAmount) {
        this.emergencyFundAmount = emergencyFundAmount;
    }

    public int getDependentsCount() {
        return dependentsCount;
    }

    public void setDependentsCount(int dependentsCount) {
        this.dependentsCount = dependentsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
