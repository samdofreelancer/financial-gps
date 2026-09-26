package com.financialgps.application.account.port.in;

import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;

/** Use case: build the deterministic owner data-export bundle (FR-011, SC-006). */
public interface ExportOwnerData {

    ExportBundle export(OwnerId owner);
}
