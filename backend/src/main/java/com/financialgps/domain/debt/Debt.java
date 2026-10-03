package com.financialgps.domain.debt;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;

import java.util.Objects;

/**
 * Debt aggregate (spec §4.1, invariants §12.1–§12.5). Immutable value object: lifecycle
 * transitions (pay off, archive) return new instances instead of mutating.
 *
 * <p>Currency is enforced single-currency by {@link Money#add} on every aggregation; the domain
 * never converts.
 *
 * <p>{@code originalPrincipal} is nullable by design (spec §4.1 "Nullable: Yes; if present
 * {@code >= 0}"): {@code null} means the origination amount is unknown and is preserved as
 * unknown — it is never silently reported as {@code 0.00}.
 */
public final class Debt {

    private final String creditor;
    private final DebtType debtType;
    private final Money originalPrincipal;
    private final Money outstandingBalance;
    private final Rate annualInterestRate;
    private final Money minimumPayment;
    private final Money plannedPayment;
    private final Integer dueDay;
    private final DebtStatus status;

    public Debt(String creditor,
                DebtType debtType,
                Money originalPrincipal,
                Money outstandingBalance,
                Rate annualInterestRate,
                Money minimumPayment,
                Money plannedPayment,
                Integer dueDay,
                DebtStatus status) {
        if (creditor == null || creditor.isBlank()) {
            throw new DomainValidationException("DEBT_CREDITOR_REQUIRED", "Creditor is required");
        }
        this.creditor = creditor;
        this.debtType = Objects.requireNonNull(debtType, "debtType");
        // originalPrincipal is nullable by design: null means "unknown", never 0.00 (spec §4.1).
        this.originalPrincipal = originalPrincipal;
        this.outstandingBalance = Objects.requireNonNull(outstandingBalance, "outstandingBalance");
        // annualInterestRate is nullable by design: null means "missing", never 0% (spec §5.3).
        this.annualInterestRate = annualInterestRate;
        this.minimumPayment = Objects.requireNonNull(minimumPayment, "minimumPayment");
        this.plannedPayment = Objects.requireNonNull(plannedPayment, "plannedPayment");
        if (dueDay != null && (dueDay < 1 || dueDay > 31)) {
            throw new DomainValidationException("DEBT_DUE_DAY_INVALID",
                    "Due day must be between 1 and 31");
        }
        this.dueDay = dueDay;
        this.status = Objects.requireNonNull(status, "status");
        validate();
    }

    /**
     * Reconstitution factory for stored debt facts (spec §4.1): builds the value objects from the
     * persistence-shaped strings. Keeping this here means no application or infrastructure mapper
     * ever has to construct domain {@link Money}/{@link Rate} itself, and the "unknown principal is
     * not zero" rule lives in exactly one place.
     *
     * @param currency single-currency MVP currency for the amount columns (spec §7)
     * @param originalPrincipal nullable — {@code null} is preserved as unknown, never coerced to 0
     */
    public static Debt reconstitute(String currency, String creditor, String debtType,
                                    String originalPrincipal, String outstandingBalance,
                                    String annualInterestRate, String minimumPayment,
                                    String plannedPayment, Integer dueDay, DebtStatus status) {
        return new Debt(creditor, DebtType.valueOf(debtType),
                originalPrincipal == null ? null : Money.of(originalPrincipal, currency),
                Money.of(outstandingBalance, currency),
                annualInterestRate == null ? null : Rate.of(annualInterestRate),
                Money.of(minimumPayment, currency),
                Money.of(plannedPayment, currency),
                dueDay, status);
    }

    /**
     * New debt fact from user input: the lifecycle is derived from the balance (spec §4.3) — a zero
     * balance is {@code PAID_OFF} with zero payments (invariant §12.4), anything else is
     * {@code ACTIVE} and must carry positive payments (invariant §12.3).
     */
    public static Debt recorded(String currency, String creditor, String debtType,
                                String originalPrincipal, String outstandingBalance,
                                String annualInterestRate, String minimumPayment,
                                String plannedPayment, Integer dueDay) {
        boolean paidOff = Money.of(outstandingBalance, currency).amount().signum() == 0;
        return reconstitute(currency, creditor, debtType, originalPrincipal, outstandingBalance,
                annualInterestRate, paidOff ? "0.00" : minimumPayment,
                paidOff ? "0.00" : plannedPayment, dueDay,
                paidOff ? DebtStatus.PAID_OFF : DebtStatus.ACTIVE);
    }

    private void validate() {
        if (status == DebtStatus.ARCHIVED) {
            return;
        }
        boolean paidOff = outstandingBalance.amount().signum() == 0;
        if (paidOff) {
            if (status != DebtStatus.PAID_OFF) {
                throw new DomainValidationException("DEBT_STATUS_INCONSISTENT",
                        "A debt with zero balance must have status PAID_OFF");
            }
            if (minimumPayment.amount().signum() != 0 || plannedPayment.amount().signum() != 0) {
                throw new DomainValidationException("DEBT_PAID_PAYMENTS_MUST_BE_ZERO",
                        "A paid-off debt must have zero minimum and planned payments");
            }
            return;
        }
        if (status != DebtStatus.ACTIVE) {
            throw new DomainValidationException("DEBT_STATUS_INCONSISTENT",
                    "A debt with positive balance must have status ACTIVE");
        }
        if (minimumPayment.amount().signum() <= 0 || plannedPayment.amount().signum() <= 0) {
            throw new DomainValidationException("DEBT_POSITIVE_BALANCE_REQUIRES_PAYMENTS",
                    "A debt with positive balance requires positive minimum and planned payments");
        }
        if (plannedPayment.amount().compareTo(minimumPayment.amount()) < 0) {
            throw new DomainValidationException("DEBT_PLANNED_BELOW_MINIMUM",
                    "Planned monthly payment cannot be less than the mandatory minimum payment");
        }
    }

    /** Transition used when the user pays the balance down to zero (spec §4.3). */
    public Debt paidOff(Money zeroBalance) {
        Objects.requireNonNull(zeroBalance, "zeroBalance");
        if (zeroBalance.amount().signum() != 0) {
            throw new DomainValidationException("DEBT_PAID_BALANCE_MUST_BE_ZERO",
                    "Paid-off transition requires a zero balance");
        }
        return new Debt(creditor, debtType, originalPrincipal, zeroBalance, annualInterestRate,
                Money.zero(minimumPayment.currency()), Money.zero(minimumPayment.currency()),
                dueDay, DebtStatus.PAID_OFF);
    }

    /** Soft-delete tombstone (spec §4.3): preserved for audit, excluded from every query. */
    public Debt archived() {
        return new Debt(creditor, debtType, originalPrincipal, outstandingBalance, annualInterestRate,
                minimumPayment, plannedPayment, dueDay, DebtStatus.ARCHIVED);
    }

    /** Active debts drive totals, DTI, projections and Mandatory Payment (spec §4.3). */
    public boolean contributesToTotals() {
        return status == DebtStatus.ACTIVE;
    }

    public String creditor() {
        return creditor;
    }

    public DebtType debtType() {
        return debtType;
    }

    public Money originalPrincipal() {
        return originalPrincipal;
    }

    public Money outstandingBalance() {
        return outstandingBalance;
    }

    public Rate annualInterestRate() {
        return annualInterestRate;
    }

    public Money minimumPayment() {
        return minimumPayment;
    }

    public Money plannedPayment() {
        return plannedPayment;
    }

    public Integer dueDay() {
        return dueDay;
    }

    public DebtStatus status() {
        return status;
    }
}
