package com.financialgps.infrastructure.persistence.ownership;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Ownership registry, realized against {@code information_schema} (plan §Data model, FR-014).
 * Every table that carries an {@code owner_id} column is discovered here, so cascade and
 * zero-orphan tests automatically cover any table a future feature adds — the registry cannot be
 * forgotten because it is derived from the schema itself.
 */
@Component
public class OwnershipQueries {

    private final JdbcTemplate jdbc;

    public OwnershipQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Names of every table in the current schema that owns an {@code owner_id} column. */
    public List<String> ownerScopedTables() {
        return jdbc.queryForList("""
                select c.table_name
                from information_schema.columns c
                join information_schema.tables t
                  on t.table_schema = c.table_schema and t.table_name = c.table_name
                where c.column_name = 'owner_id'
                  and c.table_schema = current_schema()
                  and t.table_type = 'BASE TABLE'
                order by c.table_name
                """, String.class);
    }

    /**
     * Count of rows owned by {@code ownerId} in {@code tableName}. The table name is interpolated
     * by design: it always comes from {@link #ownerScopedTables()}, never from user input.
     */
    public long countRowsForOwner(String tableName, UUID ownerId) {
        Long count = jdbc.queryForObject(
                "select count(*) from \"" + tableName + "\" where owner_id = ?", Long.class, ownerId);
        return count == null ? 0L : count;
    }
}
