package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.application.debt.port.out.DebtStore;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export adapter for the {@code debts} bundle section (spec §9.4). */
@Component
class DebtExportSection implements OwnerDataSection {

    private final DebtStore debts;

    DebtExportSection(DebtStore debts) {
        this.debts = debts;
    }

    @Override
    public String name() {
        return "debts";
    }

    @Override
    public List<ExportBundle.Row> rows(OwnerId owner) {
        List<ExportBundle.Row> rows = new ArrayList<>();
        for (DebtRecord debt : debts.findAllByOwner(owner)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("creditor", debt.creditor());
            fields.put("debtType", debt.debtType());
            fields.put("originalPrincipal", debt.originalPrincipal());
            fields.put("outstandingBalance", debt.outstandingBalance());
            if (debt.annualInterestRate() != null) {
                fields.put("annualInterestRate", debt.annualInterestRate());
            }
            fields.put("minimumPayment", debt.minimumPayment());
            fields.put("plannedPayment", debt.plannedPayment());
            fields.put("status", debt.status());
            rows.add(new ExportBundle.Row(debt.id().toString(), fields));
        }
        return List.copyOf(rows);
    }
}
