package com.financialgps.domain.debt;

/**
 * Debt lifecycle (spec §4.3). ARCHIVED is the soft-delete tombstone: rows are preserved but
 * excluded from every active query, summary, projection and cash-flow derivation.
 */
public enum DebtStatus {
    ACTIVE,
    PAID_OFF,
    ARCHIVED
}
