package com.financialgps.application.profile;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export participant: incomes section. */
@Component
public class IncomeExporter implements com.financialgps.application.account.OwnerDataExporter {

    private final com.financialgps.infrastructure.persistence.profile.IncomeRepository incomes;

    public IncomeExporter(com.financialgps.infrastructure.persistence.profile.IncomeRepository incomes) {
        this.incomes = incomes;
    }

    @Override
    public String section() {
        return "incomes";
    }

    @Override
    public List<Map<String, Object>> export(com.financialgps.application.account.OwnerId owner) {
        List<Map<String, Object>> rows = new ArrayList<>();
        incomes.findByOwnerIdOrderByCreatedAt(owner.value()).forEach(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.getId().toString());
            m.put("amount", row.getAmount().toPlainString());
            m.put("source", row.getSource());
            rows.add(m);
        });
        return rows;
    }
}
