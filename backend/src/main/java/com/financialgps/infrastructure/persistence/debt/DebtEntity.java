package com.financialgps.infrastructure.persistence.debt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Debt row: scalar facts only. Projections are recomputed, never stored. */
@Entity
@Table(name = "debt")
public class DebtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String creditor;

    @Column(name = "debt_type", nullable = false, length = 32)
    private String debtType;

    @Column(name = "original_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalPrincipal = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "annual_interest_rate", precision = 9, scale = 6)
    private BigDecimal annualInterestRate;

    @Column(name = "minimum_payment", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumPayment;

    @Column(name = "planned_payment", nullable = false, precision = 19, scale = 2)
    private BigDecimal plannedPayment;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DebtEntity() {
    }

    public DebtEntity(UUID ownerId, String creditor, String debtType, BigDecimal originalPrincipal,
                      BigDecimal outstandingBalance, BigDecimal annualInterestRate,
                      BigDecimal minimumPayment, BigDecimal plannedPayment, Integer dueDay, String status) {
        this.ownerId = ownerId;
        this.creditor = creditor;
        this.debtType = debtType;
        this.originalPrincipal = originalPrincipal;
        this.outstandingBalance = outstandingBalance;
        this.annualInterestRate = annualInterestRate;
        this.minimumPayment = minimumPayment;
        this.plannedPayment = plannedPayment;
        this.dueDay = dueDay;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getCreditor() {
        return creditor;
    }

    public void setCreditor(String creditor) {
        this.creditor = creditor;
    }

    public String getDebtType() {
        return debtType;
    }

    public void setDebtType(String debtType) {
        this.debtType = debtType;
    }

    public BigDecimal getOriginalPrincipal() {
        return originalPrincipal;
    }

    public void setOriginalPrincipal(BigDecimal originalPrincipal) {
        this.originalPrincipal = originalPrincipal;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public BigDecimal getMinimumPayment() {
        return minimumPayment;
    }

    public void setMinimumPayment(BigDecimal minimumPayment) {
        this.minimumPayment = minimumPayment;
    }

    public BigDecimal getPlannedPayment() {
        return plannedPayment;
    }

    public void setPlannedPayment(BigDecimal plannedPayment) {
        this.plannedPayment = plannedPayment;
    }

    public Integer getDueDay() {
        return dueDay;
    }

    public void setDueDay(Integer dueDay) {
        this.dueDay = dueDay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
