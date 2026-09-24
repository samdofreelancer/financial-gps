package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export adapter for the {@code expenses} bundle section (FR-011). */
@Component
class ExpenseExportSection implements OwnerDataSection {

    private final ExpenseStore expenses;

    ExpenseExportSection(ExpenseStore expenses) {
        this.expenses = expenses;
    }

    @Override
    public String name() {
        return "expenses";
    }

    @Override
    public List<ExportBundle.Row> rows(OwnerId owner) {
        List<ExportBundle.Row> rows = new ArrayList<>();
        for (ExpenseRecord expense : expenses.findAllByOwner(owner)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("amount", expense.amount());
            fields.put("category", expense.category());
            fields.put("expenseType", expense.expenseType());
            rows.add(new ExportBundle.Row(expense.id().toString(), fields));
        }
        return List.copyOf(rows);
    }
}
