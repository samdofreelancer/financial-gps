package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStore;
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
        for (Debt debt : debts.findAllByOwner(owner)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("creditor", debt.creditor());
            fields.put("debtType", debt.debtType().name());
            fields.put("originalPrincipal", debt.originalPrincipal() == null ? null : debt.originalPrincipal().asDecimalString());
            fields.put("outstandingBalance", debt.outstandingBalance().asDecimalString());
            if (debt.annualInterestRate() != null) {
                fields.put("annualInterestRate", debt.annualInterestRate().asDecimalString());
            }
            fields.put("minimumPayment", debt.minimumPayment().asDecimalString());
            fields.put("plannedPayment", debt.plannedPayment().asDecimalString());
            fields.put("dueDay", debt.dueDay());
            fields.put("status", debt.status().name());
            if (debt.paymentMarkedOn() != null) {
                fields.put("paymentMarkedOn", debt.paymentMarkedOn().toString());
            }
            rows.add(new ExportBundle.Row(debt.id() == null ? null : debt.id().value().toString(), fields));
        }
        return List.copyOf(rows);
    }
}
