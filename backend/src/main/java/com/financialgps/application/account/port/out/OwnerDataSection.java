package com.financialgps.application.account.port.out;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;

import java.util.List;

/**
 * Extension seam for the export bundle (plan §Export extension-contract clause). A future financial
 * feature registers exactly one adapter per bundle section; feature 001 contributes
 * {@code profile}/{@code incomes}/{@code expenses} from the profile context. A section with no
 * adapter serializes as {@code []} — never omitted.
 *
 * <p>Implementations are adapters, so they may query persistence directly; the export use case only
 * ever sees {@link ExportBundle.Row} values.
 */
public interface OwnerDataSection {

    /** Section name, one of {@link ExportBundle#SECTIONS}. */
    String name();

    /**
     * Rows owned by {@code owner}. Each row MUST carry an {@code id}; ordering is irrelevant here
     * because the use case sorts rows by {@code id} for determinism.
     */
    List<ExportBundle.Row> rows(OwnerId owner);
}
