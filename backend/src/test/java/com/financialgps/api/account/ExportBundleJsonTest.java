package com.financialgps.api.account;

import com.financialgps.application.account.model.ExportBundle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 step 4: the export DOCUMENT shape belongs to the API adapter, not to the use case. This
 * pins the field order that makes two exports of unchanged data byte-identical (SC-006).
 */
class ExportBundleJsonTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-25T10:00:00Z");

    @Test
    void documentFollowsTheFixedSectionOrderWithIdFirstInEveryRow() {
        Map<String, Object> profileFields = new LinkedHashMap<>();
        profileFields.put("currency", "VND");
        profileFields.put("savingsAmount", "10.00");

        ExportBundle bundle = new ExportBundle(1, CREATED_AT,
                new ExportBundle.Account("user@example.com", CREATED_AT),
                List.of(new ExportBundle.Section("profile", List.of(
                                new ExportBundle.Row("00000000-0000-0000-0000-000000000001", profileFields))),
                        new ExportBundle.Section("incomes", List.of()),
                        new ExportBundle.Section("expenses", List.of())));

        Map<String, Object> document = ExportBundleJson.document(bundle);

        assertThat(document.keySet()).containsExactly(
                "formatVersion", "exportedAt", "account", "profile", "incomes", "expenses");
        assertThat(document.get("formatVersion")).isEqualTo(1);
        assertThat(document.get("exportedAt")).isEqualTo(CREATED_AT);

        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) document.get("account");
        assertThat(account.keySet()).containsExactly("email", "createdAt");

        List<Map<String, Object>> rows = rowsOf(document, "profile");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).keySet()).as("id is rendered first").containsExactly(
                "id", "currency", "savingsAmount");
    }

    @Test
    void emptySectionsRenderAsEmptyListsNeverOmitted() {
        ExportBundle bundle = new ExportBundle(1, CREATED_AT,
                new ExportBundle.Account("user@example.com", CREATED_AT),
                ExportBundle.SECTIONS.stream()
                        .map(name -> new ExportBundle.Section(name, List.of()))
                        .toList());

        Map<String, Object> document = ExportBundleJson.document(bundle);

        for (String section : ExportBundle.SECTIONS) {
            assertThat(document.get(section)).isInstanceOf(List.class);
            assertThat((List<?>) document.get(section)).as(section).isEmpty();
        }
    }

    @Test
    void theSameBundleAlwaysRendersIdentically() {
        ExportBundle bundle = new ExportBundle(1, CREATED_AT,
                new ExportBundle.Account("user@example.com", CREATED_AT),
                List.of(new ExportBundle.Section("profile", List.of())));

        assertThat(ExportBundleJson.document(bundle)).isEqualTo(ExportBundleJson.document(bundle));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rowsOf(Map<String, Object> document, String section) {
        return (List<Map<String, Object>>) document.get(section);
    }
}
