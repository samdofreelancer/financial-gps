-- Spec-alignment follow-up: 002 §4.1 bounds that earlier migrations left open.
-- annual_interest_rate is a fraction 0..1; creditor is at most 120 characters.
-- NOT VALID: pre-existing rows are grandfathered, new and updated rows must comply
-- (domain + DTO validation reject them first with 400).
ALTER TABLE debt ADD CONSTRAINT chk_debt_rate_range
    CHECK (annual_interest_rate IS NULL OR (annual_interest_rate >= 0 AND annual_interest_rate <= 1))
    NOT VALID;
ALTER TABLE debt ADD CONSTRAINT chk_debt_creditor_max_length
    CHECK (char_length(creditor) <= 120)
    NOT VALID;
