-- 002-debt-management: V3__debt.sql.
-- Debt aggregate (spec §4.1, plan §2.3): owner-scoped, lifecycle ACTIVE/PAID_OFF/ARCHIVED.
-- Single-currency MVP: no currency column — DebtView.currency is derived from profile (spec §10.3).
-- Money: numeric(19,2); rate: numeric(9,6); DDL owned by Flyway only.

CREATE TABLE debt (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    creditor TEXT NOT NULL CHECK (char_length(creditor) > 0),
    debt_type VARCHAR(32) NOT NULL CHECK (debt_type IN ('CREDIT_CARD','MORTGAGE','AUTO_LOAN','STUDENT_LOAN','PERSONAL_LOAN','OTHER')),
    original_principal NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (original_principal >= 0),
    outstanding_balance NUMERIC(19,2) NOT NULL CHECK (outstanding_balance >= 0),
    annual_interest_rate NUMERIC(9,6),
    minimum_payment NUMERIC(19,2) NOT NULL CHECK (minimum_payment >= 0),
    planned_payment NUMERIC(19,2) NOT NULL CHECK (planned_payment >= minimum_payment),
    due_day INT CHECK (due_day BETWEEN 1 AND 31),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','PAID_OFF','ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_debt_owner ON debt (owner_id);
CREATE INDEX ix_debt_owner_status ON debt (owner_id, status);
