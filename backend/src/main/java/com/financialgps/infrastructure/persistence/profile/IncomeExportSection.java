package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export adapter for the {@code incomes} bundle section (FR-011). */
@Component
class IncomeExportSection implements OwnerDataSection {

    private final IncomeStore incomes;

    IncomeExportSection(IncomeStore incomes) {
        this.incomes = incomes;
    }

    @Override
    public String name() {
        return "incomes";
    }

    @Override
    public List<ExportBundle.Row> rows(OwnerId owner) {
        List<ExportBundle.Row> rows = new ArrayList<>();
        for (IncomeRecord income : incomes.findAllByOwner(owner)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("amount", income.amount());
            fields.put("source", income.source());
            rows.add(new ExportBundle.Row(income.id().toString(), fields));
        }
        return List.copyOf(rows);
    }
}
