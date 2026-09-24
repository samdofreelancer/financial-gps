-- Test-only fixture: two owner-scoped tables used by the ownership/cascade/matrix tests
-- (plan Deviation #2: test-fixture owned resource — NOT a production financial table).
-- They model the two shapes future financial tables take: an active resource and its archive.
CREATE TABLE test_owned_resource (
    id         UUID PRIMARY KEY,
    owner_id   UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    label      VARCHAR(255) NOT NULL,
    archived   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_test_owned_resource_owner ON test_owned_resource(owner_id);

CREATE TABLE test_owned_archive (
    id         UUID PRIMARY KEY,
    owner_id   UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    payload    VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_test_owned_archive_owner ON test_owned_archive(owner_id);
