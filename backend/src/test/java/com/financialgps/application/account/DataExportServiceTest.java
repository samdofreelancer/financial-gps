package com.financialgps.application.account;

import com.financialgps.infrastructure.persistence.account.AccountEntity;
import com.financialgps.infrastructure.persistence.account.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T015 — deterministic bundle: fixed section order, id-sorted rows, empty sections as []. */
class DataExportServiceTest {

    private static final UUID OWNER = UUID.randomUUID();

    private AccountRepository accountRepository;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        account = new AccountEntity("user@example.com", "$2a$12$hashhashhashhashhashhashhashhashhashhashhashha",
                "OWNER", Instant.parse("2026-08-25T10:00:00Z"));
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", OWNER);
        when(accountRepository.findById(OWNER)).thenReturn(Optional.of(account));
    }

    @Test
    void bundleHasFixedSectionOrderWithEmptyArrays() {
        DataExportService service = new DataExportService(accountRepository, List.of());

        Map<String, Object> bundle = service.export(new OwnerId(OWNER));

        assertThat(bundle.keySet()).containsExactly("formatVersion", "exportedAt", "account",
                "profile", "incomes", "expenses", "debts", "goals", "timelineChanges",
                "allocationRules", "gpsSnapshots", "reviewLedger");
        assertThat(bundle.get("formatVersion")).isEqualTo(1);
        for (String section : DataExportService.SECTIONS) {
            assertThat((List<?>) bundle.get(section)).as(section).isEmpty();
        }
    }

    @Test
    void exporterRowsAreSortedById() {
        OwnerDataExporter fake = new OwnerDataExporter() {
            @Override
            public String section() {
                return "incomes";
            }

            @Override
            public List<Map<String, Object>> export(OwnerId owner) {
                Map<String, Object> second = row("00000000-0000-0000-0000-000000000002", "second");
                Map<String, Object> first = row("00000000-0000-0000-0000-000000000001", "first");
                return List.of(second, first); // deliberately out of order
            }
        };
        DataExportService service = new DataExportService(accountRepository, List.of(fake));

        Map<String, Object> bundle = service.export(new OwnerId(OWNER));

        List<Map<String, Object>> incomes = asRows(bundle, "incomes");
        assertThat(incomes).hasSize(2);
        assertThat(incomes.get(0).get("id")).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(incomes.get(1).get("id")).isEqualTo("00000000-0000-0000-0000-000000000002");
    }

    @Test
    void accountSectionContainsNoCredentialMaterial() {
        DataExportService service = new DataExportService(accountRepository, List.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> bundle = service.export(new OwnerId(OWNER));

        Map<String, Object> accountSection = (Map<String, Object>) bundle.get("account");
        assertThat(accountSection).containsOnlyKeys("email", "createdAt");
        assertThat(accountSection.get("email")).isEqualTo("user@example.com");
        assertThat(bundle.toString()).doesNotContain("$2a$").doesNotContain("password");
    }

    @Test
    void unchangedDataProducesEqualBundles() {
        DataExportService service = new DataExportService(accountRepository, List.of());

        assertThat(service.export(new OwnerId(OWNER))).isEqualTo(service.export(new OwnerId(OWNER)));
    }

    @Test
    void duplicateSectionRegistrationIsRejected() {
        OwnerDataExporter a = fake("debts");
        OwnerDataExporter b = fake("debts");
        try {
            new DataExportService(accountRepository, List.of(a, b));
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).contains("debts");
        }
    }

    private OwnerDataExporter fake(String section) {
        return new OwnerDataExporter() {
            @Override
            public String section() {
                return section;
            }

            @Override
            public List<Map<String, Object>> export(OwnerId owner) {
                return List.of();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asRows(Map<String, Object> bundle, String section) {
        return (List<Map<String, Object>>) bundle.get(section);
    }

    private Map<String, Object> row(String id, String label) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", label);
        return row;
    }
}
