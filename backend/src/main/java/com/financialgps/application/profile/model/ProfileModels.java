package com.financialgps.application.profile.model;

import java.util.List;
import java.util.UUID;

/**
 * Financial Profile application models: commands in, typed results out.
 *
 * <p>Monetary values travel as plain decimal strings (calculation-rules §1) — never as JSON numbers
 * and never re-parsed by the API adapter. Every total result states how it was derived
 * ({@code provenance}: {@code actual} for stored facts, {@code calculated} for engine output), so
 * the explainability contract survives the refactor unchanged.
 */
public final class ProfileModels {

    private ProfileModels() {
    }

    /** Create-or-replace command for the owner's single profile (PUT /profile). */
    public record PutProfileCommand(String currency, String savingsAmount,
                                    String emergencyFundAmount, int dependentsCount) {
    }

    /** Income line command. {@code effectiveFrom} is supplied by the use case from the business date. */
    public record IncomeCommand(String amount, String source) {
    }

    /** Expense line command. {@code effectiveFrom} is supplied by the use case from the business date. */
    public record ExpenseCommand(String amount, String category, String expenseType) {
    }

    public record ProvenanceView(String field, String kind, String detail) {
    }

    /** A money value plus how it was obtained. */
    public record MoneyView(String amount, String currency, String provenance) {
    }

    /** A stored income line as reported to the client. */
    public record IncomeLineView(String id, String amount, String currency, String source, String provenance) {
    }

    /** A stored expense line as reported to the client. */
    public record ExpenseLineView(String id, String amount, String currency, String category,
                                  String expenseType, String provenance) {
    }

    /**
     * The Financial Profile position: stored facts plus server-calculated totals. Never carries an
     * owner or persistence identifier beyond the line {@code id} the client needs to mutate a line.
     */
    public record ProfileView(String currency, String savingsAmount,
                              String emergencyFundAmount, int dependentsCount,
                              List<IncomeLineView> incomes, List<ExpenseLineView> expenses,
                              MoneyView totalIncome, MoneyView totalExpenses,
                              MoneyView netCashFlow, MoneyView availableCapacity,
                              List<ProvenanceView> provenance, String asOf) {
    }

    /** Result of an income line mutation. */
    public record IncomeView(UUID id, String amount, String source) {
    }

    /** Result of an expense line mutation. */
    public record ExpenseView(UUID id, String amount, String category, String expenseType) {
    }
}
