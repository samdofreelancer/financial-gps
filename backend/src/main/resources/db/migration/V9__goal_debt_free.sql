-- 003-financial-goals follow-up (decision D-6): debt-freedom completion.
-- A DEBT_FREEDOM goal completes when the Feature 002 portfolio is debt-free, a condition that is
-- dynamic and not sticky (a new ACTIVE debt makes it ACTIVE again). Completion is derived on every
-- read, so no stored flag is authoritative; only ARCHIVED remains a stored terminal state.
-- target_amount/current_amount become optional advisory context for such goals.

ALTER TABLE goal ALTER COLUMN target_amount DROP NOT NULL;
ALTER TABLE goal ALTER COLUMN current_amount DROP NOT NULL;

-- V7 pinned completion_condition to AMOUNT_REACHED; allow DEBT_FREE and tie it to the goal type.
ALTER TABLE goal DROP CONSTRAINT goal_completion_condition_check;
ALTER TABLE goal ADD CONSTRAINT goal_completion_condition_check
    CHECK (completion_condition IN ('AMOUNT_REACHED', 'DEBT_FREE'));
ALTER TABLE goal ADD CONSTRAINT goal_type_completion_check
    CHECK ((goal_type = 'DEBT_FREEDOM') = (completion_condition = 'DEBT_FREE'));
