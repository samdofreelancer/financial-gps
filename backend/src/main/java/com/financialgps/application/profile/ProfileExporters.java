package com.financialgps.application.profile;

import com.financialgps.infrastructure.persistence.profile.ExpenseRepository;
import com.financialgps.infrastructure.persistence.profile.IncomeRepository;
import com.financialgps.infrastructure.persistence.profile.ProfileRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export participants: profile + incomes + expenses (sorted by id, [] when empty). */
@Component
public class ProfileExporters implements com.financialgps.application.account.OwnerDataExporter {

    private final ProfileRepository profiles;
    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;

    public ProfileExporters(ProfileRepository profiles, IncomeRepository incomes, ExpenseRepository expenses) {
        this.profiles = profiles;
        this.incomes = incomes;
        this.expenses = expenses;
    }

    @Override
    public String section() {
        return "profile";
    }

    @Override
    public List<Map<String, Object>> export(com.financialgps.application.account.OwnerId owner) {
        return profiles.findByOwnerId(owner.value()).map(profile -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", profile.getId().toString());
            row.put("currency", profile.getCurrency());
            row.put("savingsAmount", profile.getSavingsAmount().toPlainString());
            row.put("emergencyFundAmount", profile.getEmergencyFundAmount().toPlainString());
            row.put("dependentsCount", profile.getDependentsCount());
            return List.of(row);
        }).orElse(List.of());
    }
}
