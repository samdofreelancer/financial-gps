package com.financialgps.application.account.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Deterministic owner data-export bundle (FR-011, SC-006).
 *
 * <p>A typed application result: this record owns the <em>content</em> of the bundle (which sections
 * exist, in which order, with which row values) while the API adapter owns its JSON document shape
 * ({@code api.account.ExportBundleJson}). Sections are always present — an unwritten section is an
 * empty row list, never an omission, so two exports of unchanged data are byte-identical.
 */
public record ExportBundle(int formatVersion, Instant exportedAt, Account account, List<Section> sections) {

    /** Fixed bundle sections (data-model.md §Export bundle shape), in serialization order. */
    public static final List<String> SECTIONS = List.of(
            "profile", "incomes", "expenses", "debts", "goals",
            "timelineChanges", "allocationRules", "gpsSnapshots", "reviewLedger");

    /**
     * Deterministic anchor of the bundle. {@code exportedAt} is NOT the export wall-clock time but
     * the account creation instant, so the bundle is a pure function of the stored data.
     */
    public record Account(String email, Instant createdAt) {
    }

    /** One bundle section; {@code name} is one of {@link #SECTIONS}. */
    public record Section(String name, List<Row> rows) {
    }

    /**
     * One exported row. {@code id} is the deterministic ordering key and is rendered first; the
     * remaining section-specific values live in {@code fields}. Credential material never appears
     * here (SC-002).
     */
    public record Row(String id, Map<String, Object> fields) {
    }
}
