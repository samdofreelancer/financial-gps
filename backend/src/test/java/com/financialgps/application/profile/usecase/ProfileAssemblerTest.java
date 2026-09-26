package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.ProfileRecord;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 001 §A1 characterization — profile-to-engine mapping. Pins the behaviour that must survive the
 * DDD refactor: which currency reaches the engine, how inactive and not-yet-effective lines are
 * treated, what a missing profile reports, and that stored facts stay distinguishable from
 * calculated totals.
 */
class ProfileAssemblerTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 16);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());
    private static final UUID PROFILE_ID = UUID.randomUUID();

    private static ProfileRecord profile(String currency) {
        return new ProfileRecord(PROFILE_ID, OWNER, currency, "19.99", "0.01", 2);
    }

    private static IncomeRecord income(String amount, boolean active, LocalDate effectiveFrom) {
        return new IncomeRecord(UUID.randomUUID(), OWNER, PROFILE_ID, amount, "salary",
                active, effectiveFrom);
    }

    private static ExpenseRecord expense(String amount, boolean active, LocalDate effectiveFrom) {
        return new ExpenseRecord(UUID.randomUUID(), OWNER, PROFILE_ID, amount, "rent", "FIXED",
                active, effectiveFrom);
    }

    @Test
    void missingProfileIsReportedWithDefaultsAndZeroTotals() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(null, List.of(), List.of(), AS_OF);

        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.savingsAmount()).isEqualTo("0.00");
        assertThat(view.emergencyFundAmount()).isEqualTo("0.00");
        assertThat(view.dependentsCount()).isZero();
        assertThat(view.incomes()).isEmpty();
        assertThat(view.expenses()).isEmpty();
        assertThat(view.totalIncome().amount()).isEqualTo("0.00");
        assertThat(view.totalIncome().currency()).isEqualTo("VND");
        assertThat(view.asOf()).isEqualTo("2026-09-16");
    }

    @Test
    void totalsAreComputedInTheProfileCurrency() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(profile("USD"),
                List.of(income("10.00", true, AS_OF)),
                List.of(),
                AS_OF);

        assertThat(view.totalIncome().amount()).isEqualTo("10.00");
        assertThat(view.totalIncome().currency()).isEqualTo("USD");
        assertThat(view.incomes().get(0).currency()).isEqualTo("USD");
    }

    @Test
    void moneyStringsReachTheViewWithoutFloatingPointConversion() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(profile("VND"),
                List.of(income("19.99", true, AS_OF), income("0.01", true, AS_OF)),
                List.of(expense("0.30", true, AS_OF)),
                AS_OF);

        assertThat(view.totalIncome().amount())
                .isEqualTo("20.00")
                .matches("\\d+\\.\\d{2}");
        assertThat(view.netCashFlow().amount()).isEqualTo("19.70");
    }

    @Test
    void inactiveLinesAreStillListedButExcludedFromTotals() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(profile("VND"),
                List.of(income("74.00", true, AS_OF), income("99.00", false, AS_OF)),
                List.of(expense("30.00", false, AS_OF)),
                AS_OF);

        assertThat(view.incomes()).hasSize(2);
        assertThat(view.expenses()).hasSize(1);
        assertThat(view.totalIncome().amount()).isEqualTo("74.00");
        assertThat(view.totalExpenses().amount()).isEqualTo("0.00");
    }

    @Test
    void linesEffectiveInTheFutureAreListedButExcludedFromTheAsOfTotals() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(profile("VND"),
                List.of(income("50.00", true, AS_OF),
                        income("25.00", true, AS_OF.plusDays(1))),
                List.of(expense("10.00", true, AS_OF.minusDays(1))),
                AS_OF);

        assertThat(view.incomes()).hasSize(2);
        assertThat(view.totalIncome().amount()).isEqualTo("50.00");
        assertThat(view.totalExpenses().amount()).isEqualTo("10.00");
    }

    @Test
    void negativeNetCashFlowIsReportedAndCapacityClamps() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(profile("VND"),
                List.of(income("6.00", true, AS_OF)),
                List.of(expense("50.00", true, AS_OF)),
                AS_OF);

        assertThat(view.netCashFlow().amount()).isEqualTo("-44.00");
        assertThat(view.availableCapacity().amount()).isEqualTo("0.00");
    }

    @Test
    void storedFactsAreMarkedActualAndEveryTotalIsCalculated() {
        ProfileModels.ProfileView view = ProfileAssembler.assemble(profile("VND"),
                List.of(income("74.00", true, AS_OF)),
                List.of(expense("30.00", true, AS_OF)),
                AS_OF);

        assertThat(view.incomes()).allSatisfy(line -> assertThat(line.provenance()).isEqualTo("actual"));
        assertThat(view.expenses()).allSatisfy(line -> assertThat(line.provenance()).isEqualTo("actual"));
        assertThat(List.of(view.totalIncome(), view.totalExpenses(), view.netCashFlow(),
                        view.availableCapacity()))
                .allSatisfy(total -> assertThat(total.provenance()).isEqualTo("calculated"));
        assertThat(view.provenance()).extracting(ProfileModels.ProvenanceView::field)
                .containsExactly("Income", "Expense", "Net Cash Flow", "Available Capacity");
        assertThat(view.provenance()).allSatisfy(entry -> {
            assertThat(entry.kind()).isEqualTo("calculated");
            assertThat(entry.detail()).isNotBlank();
        });
    }

    @Test
    void theViewNeverLeaksAnOwnerOrPersistenceIdentifier() {
        assertThat(ProfileModels.ProfileView.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .as("DTO never leaks owner or persistence identifiers")
                .doesNotContain("ownerId", "accountId", "profileId");
        assertThat(ProfileModels.IncomeLineView.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("ownerId", "accountId", "profileId", "active", "effectiveFrom");
        assertThat(ProfileModels.ExpenseLineView.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("ownerId", "accountId", "profileId", "active", "effectiveFrom");
    }
}
