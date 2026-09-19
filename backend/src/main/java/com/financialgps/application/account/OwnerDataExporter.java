package com.financialgps.application.account;

import java.util.List;
import java.util.Map;

/**
 * Extension seam for the export bundle (plan §Export extension-contract clause). Future financial
 * features register an exporter per section; 007 ships none, so every section serializes as
 * {@code []} until they exist.
 */
public interface OwnerDataExporter {

    /** Section name, one of the fixed bundle sections (data-model.md bundle shape). */
    String section();

    /**
     * Rows owned by {@code owner}. Each row is a JSON-serializable map that MUST contain an
     * {@code id} entry; the service enforces deterministic {@code id} ordering.
     */
    List<Map<String, Object>> export(OwnerId owner);
}
