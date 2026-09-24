package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Export adapter for the {@code profile} bundle section (FR-011). A thin adapter over the
 * {@link ProfileStore} port: the export use case never sees a repository or entity (plan Phase 2
 * step 5).
 */
@Component
class ProfileExportSection implements OwnerDataSection {

    private final ProfileStore profiles;

    ProfileExportSection(ProfileStore profiles) {
        this.profiles = profiles;
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public List<ExportBundle.Row> rows(OwnerId owner) {
        Optional<ProfileRecord> profile = profiles.findByOwner(owner);
        if (profile.isEmpty()) {
            return List.of(); // an unwritten section is [] — never omitted
        }
        ProfileRecord record = profile.get();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("currency", record.currency());
        fields.put("savingsAmount", record.savingsAmount());
        fields.put("emergencyFundAmount", record.emergencyFundAmount());
        fields.put("dependentsCount", record.dependentsCount());
        List<ExportBundle.Row> rows = new ArrayList<>();
        rows.add(new ExportBundle.Row(record.id().toString(), fields));
        return List.copyOf(rows);
    }
}
