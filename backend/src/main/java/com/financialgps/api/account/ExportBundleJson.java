package com.financialgps.api.account;

import com.financialgps.application.account.model.ExportBundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the typed {@link ExportBundle} application result onto the documented JSON document
 * (data-model.md §Export bundle shape).
 *
 * <p>The use case decides WHAT is exported; this API-layer mapper decides the document SHAPE. Field
 * order is fixed — {@code formatVersion}, {@code exportedAt}, {@code account}, then the sections in
 * bundle order with {@code id} first inside every row — which is what makes two exports of unchanged
 * data byte-identical (SC-006).
 */
final class ExportBundleJson {

    private ExportBundleJson() {
    }

    static Map<String, Object> document(ExportBundle bundle) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("formatVersion", bundle.formatVersion());
        json.put("exportedAt", bundle.exportedAt());

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("email", bundle.account().email());
        account.put("createdAt", bundle.account().createdAt());
        json.put("account", account);

        for (ExportBundle.Section section : bundle.sections()) {
            json.put(section.name(), rows(section.rows()));
        }
        return json;
    }

    private static List<Map<String, Object>> rows(List<ExportBundle.Row> rows) {
        List<Map<String, Object>> rendered = new ArrayList<>();
        for (ExportBundle.Row row : rows) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("id", row.id());
            fields.putAll(row.fields());
            rendered.add(fields);
        }
        return List.copyOf(rendered);
    }
}
