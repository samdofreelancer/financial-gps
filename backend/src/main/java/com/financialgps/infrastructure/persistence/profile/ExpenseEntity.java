package com.financialgps.infrastructure.persistence.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Expense line: no math here, plain persistence state. */
@Entity
@Table(name = "expense")
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String category;

    @Column(name = "expense_type", nullable = false, length = 8)
    private String expenseType = "VARIABLE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ExpenseEntity() {
    }

    public ExpenseEntity(UUID ownerId, UUID profileId, BigDecimal amount, String category,
                         String expenseType, boolean active, LocalDate effectiveFrom) {
        this.ownerId = ownerId;
        this.profileId = profileId;
        this.amount = amount;
        this.category = category;
        this.expenseType = expenseType;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
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
