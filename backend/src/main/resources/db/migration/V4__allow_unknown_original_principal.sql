-- 002-debt-management: original principal is optional; NULL represents an unknown origination amount.
-- Keep this as a new migration rather than changing V3, which may already be applied in shared databases.
ALTER TABLE debt ALTER COLUMN original_principal DROP NOT NULL;
ALTER TABLE debt ALTER COLUMN original_principal DROP DEFAULT;