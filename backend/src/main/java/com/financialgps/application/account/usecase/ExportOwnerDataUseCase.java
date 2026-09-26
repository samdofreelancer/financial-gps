package com.financialgps.application.account.usecase;

import com.financialgps.application.account.AuthRequiredException;
import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.in.ExportOwnerData;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.AccountStore;
import com.financialgps.application.account.port.out.OwnerDataSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic data-export bundle (FR-011, SC-006): fixed section order, rows sorted by
 * {@code id}, empty sections present as {@code []} — never omitted — and {@code exportedAt} anchored
 * to the account creation instant. The bundle is therefore a pure function of the stored data: two
 * exports of unchanged data are byte-identical.
 *
 * <p>The use case knows nothing about JSON: it assembles a typed {@link ExportBundle} from the
 * registered {@link OwnerDataSection} adapters, and the API adapter decides the document shape.
 */
public final class ExportOwnerDataUseCase implements ExportOwnerData {

    private final AccountStore accounts;
    private final Map<String, OwnerDataSection> sectionsByName;

    public ExportOwnerDataUseCase(AccountStore accounts, List<OwnerDataSection> sections) {
        this.accounts = accounts;
        Map<String, OwnerDataSection> byName = new HashMap<>();
        for (OwnerDataSection section : sections) {
            OwnerDataSection previous = byName.put(section.name(), section);
            if (previous != null) {
                throw new IllegalStateException("Duplicate export section: " + section.name());
            }
        }
        this.sectionsByName = byName;
    }

    @Override
    public ExportBundle export(OwnerId owner) {
        AccountRecord account = accounts.findById(owner).orElseThrow(AuthRequiredException::new);

        List<ExportBundle.Section> sections = new ArrayList<>();
        for (String name : ExportBundle.SECTIONS) {
            sections.add(new ExportBundle.Section(name, rowsFor(name, owner)));
        }
        return new ExportBundle(1,
                account.createdAt(),
                new ExportBundle.Account(account.email(), account.createdAt()),
                List.copyOf(sections));
    }

    private List<ExportBundle.Row> rowsFor(String name, OwnerId owner) {
        OwnerDataSection section = sectionsByName.get(name);
        if (section == null) {
            return List.of();
        }
        List<ExportBundle.Row> rows = new ArrayList<>(section.rows(owner));
        rows.sort(Comparator.comparing(ExportBundle.Row::id));
        return List.copyOf(rows);
    }
}
