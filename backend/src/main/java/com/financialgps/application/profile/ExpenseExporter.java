package com.financialgps.application.profile;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export participant: expenses section. */
@Component
public class ExpenseExporter implements com.financialgps.application.account.OwnerDataExporter {

    private final com.financialgps.infrastructure.persistence.profile.ExpenseRepository expenses;

    public ExpenseExporter(com.financialgps.infrastructure.persistence.profile.ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    @Override
    public String section() {
        return "expenses";
    }

    @Override
    public List<Map<String, Object>> export(com.financialgps.application.account.OwnerId owner) {
        List<Map<String, Object>> rows = new ArrayList<>();
        expenses.findByOwnerIdOrderByCreatedAt(owner.value()).forEach(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", row.getId().toString());
            m.put("amount", row.getAmount().toPlainString());
            m.put("category", row.getCategory());
            m.put("expenseType", row.getExpenseType());
            rows.add(m);
        });
        return rows;
    }
}
