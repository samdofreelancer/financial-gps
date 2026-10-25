-- 003-financial-goals follow-up: optimistic locking (mirrors V6 for debt).
ALTER TABLE goal ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
