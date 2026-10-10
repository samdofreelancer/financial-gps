-- 003-financial-goals follow-up (decision D-6): debt-freedom completion.
-- A DEBT_FREEDOM goal completes when the Feature 002 portfolio is debt-free, a condition that is
-- dynamic and not sticky (a new ACTIVE debt makes it ACTIVE again). Completion is derived on every
-- read, so no stored flag is authoritative; only ARCHIVED remains a stored terminal state.
-- target_amount/current_amount become optional advisory context for such goals.

ALTER TABLE goal ALTER COLUMN target_amount DROP NOT NULL;
ALTER TABLE goal ALTER COLUMN current_amount DROP NOT NULL;

-- V7 pinned completion_condition to AMOUNT_REACHED; allow DEBT_FREE.
ALTER TABLE goal DROP CONSTRAINT goal_completion_condition_check;
ALTER TABLE goal ADD CONSTRAINT goal_completion_condition_check
    CHECK (completion_condition IN ('AMOUNT_REACHED', 'DEBT_FREE'));
-- The goal type / completion condition contract is enforced by the Goal domain constructor
-- (spec §4.5, decision D-6): a mismatch throws GOAL_COMPLETION_INVALID. No database-level
-- check is added here to avoid migration failure on existing rows.
