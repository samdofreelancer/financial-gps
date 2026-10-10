package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.domain.goal.Goal;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Export adapter for the {@code goals} bundle section (spec §7). */
@Component
class GoalExportSection implements OwnerDataSection {

    private final GoalStore goals;

    GoalExportSection(GoalStore goals) {
        this.goals = goals;
    }

    @Override
    public String name() {
        return "goals";
    }

    @Override
    public List<ExportBundle.Row> rows(OwnerId owner) {
        List<ExportBundle.Row> rows = new ArrayList<>();
        for (Goal goal : goals.findAllByOwner(owner)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("name", goal.name());
            fields.put("goalType", goal.goalType().name());
            // Advisory amounts may be absent for a DEBT_FREE goal (spec §4.5, D-6).
            fields.put("targetAmount",
                    goal.targetAmount() == null ? null : goal.targetAmount().asDecimalString());
            fields.put("currentAmount",
                    goal.currentAmount() == null ? null : goal.currentAmount().asDecimalString());
            if (goal.targetDate() != null) {
                fields.put("targetDate", goal.targetDate().toString());
            }
            fields.put("priority", goal.priority());
            fields.put("completionCondition", goal.completionCondition());
            fields.put("status", goal.status().name());
            rows.add(new ExportBundle.Row(
                    goal.id() == null ? null : goal.id().value().toString(), fields));
        }
        return List.copyOf(rows);
    }
}
