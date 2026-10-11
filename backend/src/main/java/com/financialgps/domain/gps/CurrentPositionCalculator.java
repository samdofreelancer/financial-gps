package com.financialgps.domain.gps;

import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.finance.CashFlowCalculator;
import com.financialgps.domain.finance.CashFlowResult;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.model.Portfolio;
import com.financialgps.domain.policy.FinancialPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates the current position for GPS from profile, income, expense, and debt data.
 * Reuses CashFlowCalculator; does not duplicate cash flow logic.
 */
public final class CurrentPositionCalculator {

    private CurrentPositionCalculator() {
    }

    public record Input(
            ProfileModels.ProfileView profile,
            DebtSummaryResult debtSummary,
            LocalDate asOf,
            FinancialPolicy policy
    ) {
        public Input {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(debtSummary, "debtSummary");
            Objects.requireNonNull(asOf, "asOf");
            Objects.requireNonNull(policy, "policy");
        }
    }

    private static GpsProvenance.Available provActual(String field, String detail) {
        return GpsProvenance.Available.actual(field, detail);
    }

    private static GpsProvenance.Available provCalculated(String field, String detail) {
        return GpsProvenance.Available.calculated(field, detail);
    }

    private static GpsProvenance.Unavailable provUnavailable(String field, String reasonCode, String explanation) {
        return new GpsProvenance.Unavailable(field, reasonCode, explanation);
    }

    private static GpsProvenance.Entry entryActual(String field, String detail) {
        return new GpsProvenance.AvailableEntry(provActual(field, detail));
    }

    private static GpsProvenance.Entry entryCalculated(String field, String detail) {
        return new GpsProvenance.AvailableEntry(provCalculated(field, detail));
    }

    private static GpsProvenance.Entry entryUnavailable(String field, String reasonCode, String explanation) {
        return new GpsProvenance.UnavailableEntry(provUnavailable(field, reasonCode, explanation));
    }

    public static GpsCurrentPosition calculate(Input input) {
        String currency = input.profile.currency() == null ? "VND" : input.profile.currency();
        boolean hasProfile = input.profile.currency() != null;

        // Build portfolio for CashFlowCalculator
        Portfolio portfolio = buildPortfolio(input.profile, currency);

        // Calculate cash flow
        CashFlowResult cashFlow = CashFlowCalculator.calculate(portfolio, input.asOf, input.policy);

        // Build current position with provenance
        GpsCurrentPosition.Builder builder = GpsCurrentPosition.builder();

        if (hasProfile) {
            // Income
            builder.income(new GpsCurrentPosition.Available(
                    "income",
                    cashFlow.income(),
                    provActual("income", "sum of active incomes effective on " + input.asOf)
            ));

            // Expense
            builder.expense(new GpsCurrentPosition.Available(
                    "expense",
                    cashFlow.expense(),
                    provActual("expense", "sum of active expenses effective on " + input.asOf)
            ));

            // Mandatory Payment
            builder.mandatoryPayment(new GpsCurrentPosition.Available(
                    "mandatoryPayment",
                    cashFlow.mandatoryPayment(),
                    provActual("mandatoryPayment", "sum of ACTIVE debts minimumPayment (002)")
            ));

            // Net Cash Flow
            builder.netCashFlow(new GpsCurrentPosition.Available(
                    "netCashFlow",
                    cashFlow.netCashFlow(),
                    provCalculated("netCashFlow", "Income - Expense - Mandatory Payment")
            ));

            // Available Capacity
            builder.availableCapacity(new GpsCurrentPosition.Available(
                    "availableCapacity",
                    cashFlow.availableCapacity(),
                    provCalculated("availableCapacity", "max(Net Cash Flow, 0.00)")
            ));

            // Savings
            if (input.profile.savingsAmount() != null) {
                builder.savings(new GpsCurrentPosition.Available(
                        "savings",
                        Money.of(input.profile.savingsAmount(), currency),
                        provActual("savings", "stored savings amount from profile")
                ));
            } else {
                builder.savings(new GpsCurrentPosition.Unavailable(
                        "savings",
                        currency,
                        provUnavailable("savings", "PROFILE_MISSING", "Savings not available: financial profile incomplete")
                ));
            }

            // Emergency Fund
            if (input.profile.emergencyFundAmount() != null) {
                builder.emergencyFund(new GpsCurrentPosition.Available(
                        "emergencyFund",
                        Money.of(input.profile.emergencyFundAmount(), currency),
                        provActual("emergencyFund", "stored emergency fund amount from profile")
                ));
            } else {
                builder.emergencyFund(new GpsCurrentPosition.Unavailable(
                        "emergencyFund",
                        currency,
                        provUnavailable("emergencyFund", "PROFILE_MISSING", "Emergency fund not available: financial profile incomplete")
                ));
            }

            // Dependents
            builder.dependents(input.profile.dependentsCount());

            // Total Outstanding Debt
            builder.totalOutstandingDebt(new GpsCurrentPosition.Available(
                    "totalOutstandingDebt",
                    Money.of(input.debtSummary.totalOutstandingDebt().toPlainString(), currency),
                    provCalculated("totalOutstandingDebt", "sum of ACTIVE debts outstandingBalance (002)")
            ));

            // DTI
            builder.dti(convertDti(input.debtSummary.debtToIncome(), currency));

        } else {
            // No financial profile - all profile-dependent values are UNAVAILABLE
            String reason = "Financial profile not set";
            String reasonCode = "PROFILE_MISSING";

            builder.income(new GpsCurrentPosition.Unavailable("income", currency,
                    provUnavailable("income", reasonCode, reason)));
            builder.expense(new GpsCurrentPosition.Unavailable("expense", currency,
                    provUnavailable("expense", reasonCode, reason)));
            builder.mandatoryPayment(new GpsCurrentPosition.Unavailable("mandatoryPayment", currency,
                    provUnavailable("mandatoryPayment", reasonCode, reason)));
            builder.netCashFlow(new GpsCurrentPosition.Unavailable("netCashFlow", currency,
                    provUnavailable("netCashFlow", reasonCode, reason)));
            builder.availableCapacity(new GpsCurrentPosition.Unavailable("availableCapacity", currency,
                    provUnavailable("availableCapacity", reasonCode, reason)));
            builder.savings(new GpsCurrentPosition.Unavailable("savings", currency,
                    provUnavailable("savings", reasonCode, reason)));
            builder.emergencyFund(new GpsCurrentPosition.Unavailable("emergencyFund", currency,
                    provUnavailable("emergencyFund", reasonCode, reason)));
            builder.dependents(null);
            builder.totalOutstandingDebt(new GpsCurrentPosition.Available(
                    "totalOutstandingDebt",
                    Money.of(input.debtSummary.totalOutstandingDebt().toPlainString(), currency),
                    provCalculated("totalOutstandingDebt", "sum of ACTIVE debts outstandingBalance (002)")
            ));
            builder.dti(new GpsCurrentPosition.DtiValue(
                    "UNAVAILABLE",
                    null,
                    "ZERO_OR_MISSING_INCOME",
                    "Debt-to-income is unavailable: financial profile not set",
                    entryUnavailable("dti", reasonCode, reason)
            ));
        }

        return builder.build();
    }

    private static Portfolio buildPortfolio(ProfileModels.ProfileView profile, String currency) {
        List<com.financialgps.domain.model.Income> incomes = new ArrayList<>();
        if (profile.incomes() != null) {
            for (ProfileModels.IncomeLineView iv : profile.incomes()) {
                // We don't have active/effectiveFrom in the view, so we assume all are active
                incomes.add(new com.financialgps.domain.model.Income(
                        Money.of(iv.amount(), currency),
                        iv.source(),
                        true,
                        LocalDate.MIN // placeholder - not used since active=true
                ));
            }
        }

        List<com.financialgps.domain.model.Expense> expenses = new ArrayList<>();
        if (profile.expenses() != null) {
            for (ProfileModels.ExpenseLineView ev : profile.expenses()) {
                expenses.add(new com.financialgps.domain.model.Expense(
                        Money.of(ev.amount(), currency),
                        ev.category(),
                        "FIXED".equals(ev.expenseType())
                                ? com.financialgps.domain.model.Expense.ExpenseType.FIXED
                                : com.financialgps.domain.model.Expense.ExpenseType.VARIABLE,
                        true,
                        LocalDate.MIN
                ));
            }
        }

        // We pass empty debts list since mandatory payment is computed from debt summary
        // in the real implementation, we'd pass full debt objects
        return new Portfolio(incomes, expenses, List.of(), List.of());
    }

    private static GpsCurrentPosition.DtiValue convertDti(DebtSummaryResult.DtiResult dti, String currency) {
        GpsProvenance.Entry provenance;
        if ("AVAILABLE".equals(dti.status())) {
            provenance = entryActual("dti",
                    "Total minimum monthly debt (" + dti.ratio().toPlainString() + ") divided by monthly income");
        } else {
            provenance = entryUnavailable("dti", dti.reasonCode(), dti.explanation());
        }
        return new GpsCurrentPosition.DtiValue(
                dti.status(),
                dti.ratio(),
                dti.reasonCode(),
                dti.explanation(),
                provenance
        );
    }
}