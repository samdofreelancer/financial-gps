package com.financialgps.domain.debt;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Debt aggregate root (spec §4.1, invariants §12.1–§12.5). Immutable; lifecycle transitions
 * (pay off, archive, payment mark) return new instances instead of mutating. Identity is
 * {@link DebtId} — {@code null} only for a not-yet-persisted debt.
 *
 * <p>Reconstitution ({@link #reconstitute}) is the single place where persistence strings become
 * value objects, so the "unknown principal is not zero" rule lives in exactly one place.
 */
public final class Debt {

    /** Single-currency MVP: every debt is expressed in the owner's profile currency (default VND). */
    public static final String DEFAULT_CURRENCY = "VND";

    private final DebtId id;
    private final String creditor;
    private final DebtType debtType;
    private final Money originalPrincipal;
    private final Money outstandingBalance;
    private final Rate annualInterestRate;
    private final Money minimumPayment;
    private final Money plannedPayment;
    private final Integer dueDay;
    private final DebtStatus status;
    private final LocalDate paymentMarkedOn;

    public Debt(String creditor,
                DebtType debtType,
                Money originalPrincipal,
                Money outstandingBalance,
                Rate annualInterestRate,
                Money minimumPayment,
                Money plannedPayment,
                Integer dueDay,
                DebtStatus status) {
        this(null, null, creditor, debtType, originalPrincipal, outstandingBalance, annualInterestRate,
                minimumPayment, plannedPayment, dueDay, status);
    }

    private Debt(DebtId id, LocalDate paymentMarkedOn, String creditor, DebtType debtType,
                 Money originalPrincipal, Money outstandingBalance, Rate annualInterestRate,
                 Money minimumPayment, Money plannedPayment, Integer dueDay, DebtStatus status) {
        if (creditor == null || creditor.isBlank()) {
            throw new DomainValidationException("DEBT_CREDITOR_REQUIRED", "Creditor is required");
        }
        this.id = id;
        this.paymentMarkedOn = paymentMarkedOn;
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

    /** Reconstitution for a persisted row: the stored id and payment mark are preserved. */
    public static Debt reconstitute(DebtId id, String currency, String creditor, String debtType,
                                    String originalPrincipal, String outstandingBalance,
                                    String annualInterestRate, String minimumPayment,
                                    String plannedPayment, Integer dueDay, DebtStatus status,
                                    LocalDate paymentMarkedOn) {
        return new Debt(id, paymentMarkedOn, creditor, DebtType.valueOf(debtType),
                originalPrincipal == null ? null : Money.of(originalPrincipal, currency),
                Money.of(outstandingBalance, currency),
                annualInterestRate == null ? null : Rate.of(annualInterestRate),
                Money.of(minimumPayment, currency),
                Money.of(plannedPayment, currency),
                dueDay, status);
    }

    /** Reconstitution for an unpersisted/domain-only debt (id and payment mark unknown). */
    public static Debt reconstitute(String currency, String creditor, String debtType,
                                    String originalPrincipal, String outstandingBalance,
                                    String annualInterestRate, String minimumPayment,
                                    String plannedPayment, Integer dueDay, DebtStatus status) {
        return reconstitute(null, currency, creditor, debtType, originalPrincipal, outstandingBalance,
                annualInterestRate, minimumPayment, plannedPayment, dueDay, status, null);
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
        return new Debt(id, paymentMarkedOn, creditor, debtType, originalPrincipal, zeroBalance,
                annualInterestRate, Money.zero(minimumPayment.currency()),
                Money.zero(minimumPayment.currency()), dueDay, DebtStatus.PAID_OFF);
    }

    /** Soft-delete tombstone (spec §4.3): preserved for audit, excluded from every query. */
    public Debt archived() {
        return new Debt(id, paymentMarkedOn, creditor, debtType, originalPrincipal, outstandingBalance,
                annualInterestRate, minimumPayment, plannedPayment, dueDay, DebtStatus.ARCHIVED);
    }

    /** Attach a persisted id (used by the store when a new debt is first saved). */
    public Debt withId(DebtId id) {
        return new Debt(Objects.requireNonNull(id, "id"), paymentMarkedOn, creditor, debtType,
                originalPrincipal, outstandingBalance, annualInterestRate, minimumPayment,
                plannedPayment, dueDay, status);
    }

    /** Re-express every monetary amount in another currency (numeric values preserved). */
    public Debt withCurrency(String currency) {
        return new Debt(id, paymentMarkedOn, creditor, debtType,
                originalPrincipal == null ? null : Money.of(originalPrincipal.asDecimalString(), currency),
                Money.of(outstandingBalance.asDecimalString(), currency),
                annualInterestRate,
                Money.of(minimumPayment.asDecimalString(), currency),
                Money.of(plannedPayment.asDecimalString(), currency),
                dueDay, status);
    }

    /** Manual current-period payment marker (no funds move; null clears it). */
    public Debt withPaymentMarkedOn(LocalDate markedOn) {
        return new Debt(id, markedOn, creditor, debtType, originalPrincipal, outstandingBalance,
                annualInterestRate, minimumPayment, plannedPayment, dueDay, status);
    }

    /** Active debts drive totals, DTI, projections and Mandatory Payment (spec §4.3). */
    public boolean contributesToTotals() {
        return status == DebtStatus.ACTIVE;
    }

    public DebtId id() {
        return id;
    }

    public LocalDate paymentMarkedOn() {
        return paymentMarkedOn;
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
