package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import com.financialgps.platform.security.AuthRequiredException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic data-export bundle (FR-011, SC-006): fixed section order, rows sorted by
 * {@code id}, empty sections serialize as {@code []} — never omitted — and {@code exportedAt} is
 * anchored to the account creation instant. The bundle is therefore a pure function of the stored
 * data: two exports of unchanged data are byte-identical.
 */
@Service
public class DataExportService {

    /** Fixed bundle sections (data-model.md §Export bundle shape). */
    public static final List<String> SECTIONS = List.of(
            "profile", "incomes", "expenses", "debts", "goals",
            "timelineChanges", "allocationRules", "gpsSnapshots", "reviewLedger");

    private final AccountRepository accountRepository;
    private final Map<String, OwnerDataExporter> exportersBySection;

    public DataExportService(AccountRepository accountRepository, List<OwnerDataExporter> exporters) {
        this.accountRepository = accountRepository;
        Map<String, OwnerDataExporter> bySection = new HashMap<>();
        for (OwnerDataExporter exporter : exporters) {
            OwnerDataExporter previous = bySection.put(exporter.section(), exporter);
            if (previous != null) {
                throw new IllegalStateException("Duplicate export section: " + exporter.section());
            }
        }
        this.exportersBySection = bySection;
    }

    public Map<String, Object> export(OwnerId owner) {
        AccountEntity account = accountRepository.findById(owner.value())
                .orElseThrow(AuthRequiredException::new);

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("formatVersion", 1);
        // Deterministic anchor: NOT the export wall-clock time, so unchanged data yields
        // byte-identical bundles (quickstart scenario 7 / SC-006).
        bundle.put("exportedAt", account.getCreatedAt());

        Map<String, Object> accountSection = new LinkedHashMap<>();
        accountSection.put("email", account.getEmail());
        accountSection.put("createdAt", account.getCreatedAt());
        bundle.put("account", accountSection);

        for (String section : SECTIONS) {
            bundle.put(section, rowsFor(section, owner));
        }
        return bundle;
    }

    private List<Map<String, Object>> rowsFor(String section, OwnerId owner) {
        OwnerDataExporter exporter = exportersBySection.get(section);
        if (exporter == null) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>(exporter.export(owner));
        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("id"))));
        return rows;
    }
}
