package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Current financial position as seen by the GPS.
 * Each monetary value carries provenance and availability.
 */
public final class GpsCurrentPosition {

    /** Provenance-carrying money value (available or unavailable). */
    public interface MoneyValue {
        String field();

        String currency();
    }

    /** An available money value with provenance. */
    public record Available(
            String field,
            Money amount,
            GpsProvenance.Available provenance
    ) implements MoneyValue {
        public Available {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(provenance, "provenance");
        }

        @Override
        public String currency() {
            return amount.currency();
        }
    }

    /** An unavailable money value with reason. */
    public record Unavailable(
            String field,
            String currency,
            GpsProvenance.Unavailable provenance
    ) implements MoneyValue {
        public Unavailable {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record DtiValue(
            String status,          // "AVAILABLE" | "UNAVAILABLE"
            BigDecimal ratio,       // null when unavailable
            String reasonCode,      // null when available
            String explanation,     // null when available
            GpsProvenance.Entry provenance
    ) {
        public DtiValue {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public static final class Builder {
        private MoneyValue income;
        private MoneyValue expense;
        private MoneyValue mandatoryPayment;
        private MoneyValue netCashFlow;
        private MoneyValue availableCapacity;
        private MoneyValue savings;
        private MoneyValue emergencyFund;
        private Integer dependents;
        private MoneyValue totalOutstandingDebt;
        private DtiValue dti;

        public Builder() {
            this.income = null;
            this.expense = null;
            this.mandatoryPayment = null;
            this.netCashFlow = null;
            this.availableCapacity = null;
            this.savings = null;
            this.emergencyFund = null;
            this.dependents = null;
            this.totalOutstandingDebt = null;
            this.dti = null;
        }

        public Builder income(MoneyValue value) {
            this.income = value;
            return this;
        }

        public Builder expense(MoneyValue value) {
            this.expense = value;
            return this;
        }

        public Builder mandatoryPayment(MoneyValue value) {
            this.mandatoryPayment = value;
            return this;
        }

        public Builder netCashFlow(MoneyValue value) {
            this.netCashFlow = value;
            return this;
        }

        public Builder availableCapacity(MoneyValue value) {
            this.availableCapacity = value;
            return this;
        }

        public Builder savings(MoneyValue value) {
            this.savings = value;
            return this;
        }

        public Builder emergencyFund(MoneyValue value) {
            this.emergencyFund = value;
            return this;
        }

        public Builder dependents(Integer value) {
            this.dependents = value;
            return this;
        }

        public Builder totalOutstandingDebt(MoneyValue value) {
            this.totalOutstandingDebt = value;
            return this;
        }

        public Builder dti(DtiValue value) {
            this.dti = value;
            return this;
        }

        public GpsCurrentPosition build() {
            return new GpsCurrentPosition(
                    income, expense, mandatoryPayment, netCashFlow, availableCapacity,
                    savings, emergencyFund, dependents, totalOutstandingDebt, dti);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final MoneyValue income;
    private final MoneyValue expense;
    private final MoneyValue mandatoryPayment;
    private final MoneyValue netCashFlow;
    private final MoneyValue availableCapacity;
    private final MoneyValue savings;
    private final MoneyValue emergencyFund;
    private final Integer dependents;
    private final MoneyValue totalOutstandingDebt;
    private final DtiValue dti;

    private GpsCurrentPosition(
            MoneyValue income,
            MoneyValue expense,
            MoneyValue mandatoryPayment,
            MoneyValue netCashFlow,
            MoneyValue availableCapacity,
            MoneyValue savings,
            MoneyValue emergencyFund,
            Integer dependents,
            MoneyValue totalOutstandingDebt,
            DtiValue dti) {
        this.income = income;
        this.expense = expense;
        this.mandatoryPayment = mandatoryPayment;
        this.netCashFlow = netCashFlow;
        this.availableCapacity = availableCapacity;
        this.savings = savings;
        this.emergencyFund = emergencyFund;
        this.dependents = dependents;
        this.totalOutstandingDebt = totalOutstandingDebt;
        this.dti = dti;
    }

    public MoneyValue income() {
        return income;
    }

    public MoneyValue expense() {
        return expense;
    }

    public MoneyValue mandatoryPayment() {
        return mandatoryPayment;
    }

    public MoneyValue netCashFlow() {
        return netCashFlow;
    }

    public MoneyValue availableCapacity() {
        return availableCapacity;
    }

    public MoneyValue savings() {
        return savings;
    }

    public MoneyValue emergencyFund() {
        return emergencyFund;
    }

    public Integer dependents() {
        return dependents;
    }

    public MoneyValue totalOutstandingDebt() {
        return totalOutstandingDebt;
    }

    public DtiValue dti() {
        return dti;
    }

    /** All money values as a flat list for serialization. */
    public List<MoneyValue> allMoneyValues() {
        return List.of(income, expense, mandatoryPayment, netCashFlow, availableCapacity,
                savings, emergencyFund, totalOutstandingDebt);
    }
}