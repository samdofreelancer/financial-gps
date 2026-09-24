-- 001-financial-profile (M1 approved): V2__profile.sql.
-- One profile per owner; income/expense lines owner-scoped with profile link.
-- Money: numeric(19,2); all amounts CHECK >= 0; DDL owned by Flyway only.

CREATE TABLE profile (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    savings_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (savings_amount >= 0),
    emergency_fund_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (emergency_fund_amount >= 0),
    dependents_count INT NOT NULL DEFAULT 0 CHECK (dependents_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_profile_owner UNIQUE (owner_id)
);

CREATE INDEX ix_profile_owner ON profile (owner_id);

CREATE TABLE income (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profile(id) ON DELETE CASCADE,
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    source TEXT NOT NULL CHECK (char_length(source) > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_income_owner ON income (owner_id);
CREATE INDEX ix_income_profile ON income (profile_id);

CREATE TABLE expense (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profile(id) ON DELETE CASCADE,
    amount NUMERIC(19,2) NOT NULL CHECK (amount >= 0),
    category TEXT NOT NULL CHECK (char_length(category) > 0),
    expense_type VARCHAR(8) NOT NULL CHECK (expense_type IN ('FIXED','VARIABLE')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_expense_owner ON expense (owner_id);
CREATE INDEX ix_expense_profile ON expense (profile_id);
