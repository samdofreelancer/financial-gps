package com.financialgps.domain.debt;

/** Debt product type (spec §4.1). Stored as VARCHAR(32) in persistence; domain uses this enum. */
public enum DebtType {
    CREDIT_CARD,
    MORTGAGE,
    AUTO_LOAN,
    STUDENT_LOAN,
    PERSONAL_LOAN,
    OTHER
}
