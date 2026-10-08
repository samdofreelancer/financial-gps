-- 003-financial-goals: V7__goal.sql.
-- Goal aggregate (spec §4.1, plan §5): owner-scoped, lifecycle ACTIVE/COMPLETED/ARCHIVED.
-- Single-currency MVP: no currency column — GoalView.currency is derived from profile (spec §4.1).
-- Money: numeric(19,2); DDL owned by Flyway only.

CREATE TABLE goal (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK (char_length(name) > 0 AND char_length(name) <= 120),
    goal_type VARCHAR(32) NOT NULL CHECK (goal_type IN ('DEBT_FREEDOM','EMERGENCY_FUND','SAVINGS','HOUSING','EDUCATION','RETIREMENT','OTHER')),
    target_amount NUMERIC(19,2) NOT NULL CHECK (target_amount >= 0),
    current_amount NUMERIC(19,2) NOT NULL CHECK (current_amount >= 0),
    target_date DATE,
    priority INT NOT NULL DEFAULT 1 CHECK (priority >= 1),
    completion_condition VARCHAR(32) NOT NULL DEFAULT 'AMOUNT_REACHED' CHECK (completion_condition = 'AMOUNT_REACHED'),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','COMPLETED','ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_goal_owner ON goal (owner_id);
CREATE INDEX ix_goal_owner_status ON goal (owner_id, status);
