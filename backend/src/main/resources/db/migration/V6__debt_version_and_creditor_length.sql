-- 002-debt-management follow-up: optimistic locking + bounded creditor length.
ALTER TABLE debt ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE debt ADD CONSTRAINT chk_debt_creditor_length CHECK (char_length(creditor) <= 200);
