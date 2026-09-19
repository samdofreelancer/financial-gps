package com.financialgps.application.profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProfileModels {

    private ProfileModels() {
    }

    public record PutProfileCommand(String currency, BigDecimal savingsAmount,
                                    BigDecimal emergencyFundAmount, int dependentsCount) {
    }

    public record IncomeCommand(BigDecimal amount, String source, LocalDate effectiveFrom) {
    }

    public record ExpenseCommand(BigDecimal amount, String category, String expenseType, LocalDate effectiveFrom) {
    }

    public record ProvenanceView(String field, String kind, String detail) {
    }

    public record ProfileView(String currency, String savingsAmount,
                              String emergencyFundAmount, int dependentsCount,
                              List<Map<String, String>> incomes, List<Map<String, String>> expenses,
                              Map<String, String> totalIncome, Map<String, String> totalExpenses,
                              Map<String, String> netCashFlow, Map<String, String> availableCapacity,
                              List<ProvenanceView> provenance, String asOf) {
    }

    public record IncomeView(UUID id, String amount, String source) {
    }

    public record ExpenseView(UUID id, String amount, String category, String expenseType) {
    }
}
