package com.financialgps.application.account.usecase;

import com.financialgps.application.account.AuthRequiredException;
import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.out.AccountRecord;
import com.financialgps.application.account.port.out.OwnerDataSection;
import com.financialgps.testsupport.FakeAccountStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T015/FR-011/SC-006 — deterministic bundle: fixed section order, id-sorted rows, empty sections as
 * [], and no credential material. The use case is driven by fake {@link OwnerDataSection} output
 * ports: no repository and no entity takes part.
 */
class ExportOwnerDataUseCaseTest {

    private static final UUID OWNER = UUID.randomUUID();

    private FakeAccountStore accounts;
    private OwnerId owner;

    @BeforeEach
    void setUp() {
        accounts = new FakeAccountStore();
        OwnerId stored = new OwnerId(accounts.insert(new AccountRecord(null, "user@example.com",
                "$fake$hashhashhashhashhashhashhashhashhashhash", "OWNER",
                Instant.parse("2026-08-25T10:00:00Z"))).id());
        owner = stored;
    }

    @Test
    void bundleHasFixedSectionOrderWithEmptySections() {
        ExportBundle bundle = new ExportOwnerDataUseCase(accounts, List.of()).export(owner);

        assertThat(bundle.sections()).extracting(ExportBundle.Section::name)
                .containsExactlyElementsOf(ExportBundle.SECTIONS);
        assertThat(bundle.formatVersion()).isEqualTo(1);
        assertThat(bundle.sections()).allSatisfy(section -> assertThat(section.rows()).isEmpty());
    }

    @Test
    void exportedAtIsAnchoredToTheAccountCreationInstantNotTheWallClock() {
        ExportBundle first = new ExportOwnerDataUseCase(accounts, List.of()).export(owner);
        ExportBundle second = new ExportOwnerDataUseCase(accounts, List.of()).export(owner);

        assertThat(first.exportedAt()).isEqualTo(Instant.parse("2026-08-25T10:00:00Z"));
        assertThat(first.account().createdAt()).isEqualTo(first.exportedAt());
        assertThat(second).isEqualTo(first);
    }

    @Test
    void sectionRowsAreSortedById() {
        OwnerDataSection fake = section("incomes", List.of(
                row("00000000-0000-0000-0000-000000000002", "second"),
                row("00000000-0000-0000-0000-000000000001", "first")));

        ExportBundle bundle = new ExportOwnerDataUseCase(accounts, List.of(fake)).export(owner);

        assertThat(rowsOf(bundle, "incomes")).extracting(ExportBundle.Row::id)
                .containsExactly("00000000-0000-0000-0000-000000000001",
                        "00000000-0000-0000-0000-000000000002");
    }

    @Test
    void accountSectionCarriesNoCredentialMaterial() {
        ExportBundle bundle = new ExportOwnerDataUseCase(accounts, List.of()).export(owner);

        assertThat(bundle.account().email()).isEqualTo("user@example.com");
        assertThat(bundle.toString()).doesNotContain("$fake$").doesNotContain("password");
    }

    @Test
    void duplicateSectionRegistrationIsRejected() {
        OwnerDataSection a = section("debts", List.of());
        OwnerDataSection b = section("debts", List.of());

        try {
            new ExportOwnerDataUseCase(accounts, List.of(a, b));
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).contains("debts");
        }
    }

    @Test
    void anotherOrUnknownOwnerIsUnauthenticated() {
        try {
            new ExportOwnerDataUseCase(accounts, List.of()).export(new OwnerId(UUID.randomUUID()));
            throw new AssertionError("expected AuthRequiredException");
        } catch (AuthRequiredException expected) {
            assertThat(expected).isNotNull();
        }
    }

    private static OwnerDataSection section(String name, List<ExportBundle.Row> rows) {
        return new OwnerDataSection() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public List<ExportBundle.Row> rows(OwnerId owner) {
                return rows;
            }
        };
    }

    private static ExportBundle.Row row(String id, String label) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("label", label);
        return new ExportBundle.Row(id, fields);
    }

    private static List<ExportBundle.Row> rowsOf(ExportBundle bundle, String name) {
        return bundle.sections().stream()
                .filter(section -> section.name().equals(name))
                .findFirst()
                .orElseThrow()
                .rows();
    }
}
