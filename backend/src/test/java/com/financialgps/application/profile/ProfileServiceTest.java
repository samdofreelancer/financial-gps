package com.financialgps.application.profile;

import com.financialgps.application.account.OwnerId;
import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.infrastructure.persistence.profile.ExpenseEntity;
import com.financialgps.infrastructure.persistence.profile.ExpenseRepository;
import com.financialgps.infrastructure.persistence.profile.IncomeEntity;
import com.financialgps.infrastructure.persistence.profile.IncomeRepository;
import com.financialgps.infrastructure.persistence.profile.ProfileEntity;
import com.financialgps.infrastructure.persistence.profile.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 001 TDD RED (Slice 3 / A1): application orchestration only — owner-scoped loading, mapping to the
 * canonical engine, and not-found semantics. No formula is asserted here that the domain does not
 * already own.
 */
class ProfileServiceTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 16);

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final IncomeRepository incomes = mock(IncomeRepository.class);
    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final ProfileService service = new ProfileService(profiles, incomes, expenses);

    private final UUID ownerId = UUID.randomUUID();
    private final OwnerId owner = new OwnerId(ownerId);

    private ProfileEntity profile() {
        return new ProfileEntity(ownerId, "VND", new BigDecimal("19.99"), new BigDecimal("0.01"), 2);
    }

    private IncomeEntity income(String amount, String source, boolean active) {
        IncomeEntity entity = new IncomeEntity(ownerId, UUID.randomUUID(), new BigDecimal(amount),
                source, active, AS_OF);
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    private ExpenseEntity expense(String amount, String category, boolean active) {
        ExpenseEntity entity = new ExpenseEntity(ownerId, UUID.randomUUID(), new BigDecimal(amount),
                category, "FIXED", active, AS_OF);
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    private void stubProfileWithLines(List<IncomeEntity> incomeRows, List<ExpenseEntity> expenseRows) {
        when(profiles.findByOwnerId(ownerId)).thenReturn(Optional.of(profile()));
        when(incomes.findByProfileIdAndOwnerId(any(), eq(ownerId))).thenReturn(incomeRows);
        when(expenses.findByProfileIdAndOwnerId(any(), eq(ownerId))).thenReturn(expenseRows);
    }

    @Test
    void getProfileMapsStoredFactsThroughTheCanonicalEngine() {
        stubProfileWithLines(
                List.of(income("19.99", "salary", true), income("0.01", "side", true)),
                List.of(expense("0.30", "rent", true)));

        ProfileModels.ProfileView view = service.getProfile(owner, AS_OF);

        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.savingsAmount()).isEqualTo("19.99");
        assertThat(view.emergencyFundAmount()).isEqualTo("0.01");
        assertThat(view.dependentsCount()).isEqualTo(2);
        assertThat(view.totalIncome()).containsEntry("amount", "20.00")
                .containsEntry("provenance", "calculated");
        assertThat(view.totalExpenses()).containsEntry("amount", "0.30");
        assertThat(view.netCashFlow()).containsEntry("amount", "19.70");
        assertThat(view.availableCapacity()).containsEntry("amount", "19.70");
        assertThat(view.asOf()).isEqualTo(AS_OF.toString());
        assertThat(view.incomes()).hasSize(2)
                .allSatisfy(line -> assertThat(line).containsEntry("provenance", "actual"));
    }

    @Test
    void negativeNetCashFlowIsReportedAndCapacityClampsToZero() {
        stubProfileWithLines(List.of(income("20.00", "salary", true)),
                List.of(expense("30.00", "rent", true)));

        ProfileModels.ProfileView view = service.getProfile(owner, AS_OF);

        assertThat(view.netCashFlow()).containsEntry("amount", "-10.00");
        assertThat(view.availableCapacity()).containsEntry("amount", "0.00");
    }

    @Test
    void inactiveLinesAreExcludedByTheDomainNotByTheService() {
        stubProfileWithLines(
                List.of(income("100.00", "old job", false), income("50.00", "salary", true)),
                List.of(expense("999.00", "old rent", false)));

        ProfileModels.ProfileView view = service.getProfile(owner, AS_OF);

        assertThat(view.totalIncome()).containsEntry("amount", "50.00");
        assertThat(view.totalExpenses()).containsEntry("amount", "0.00");
        assertThat(view.incomes()).as("history stays visible as facts").hasSize(2);
    }

    @Test
    void emptyOwnerGetsZeroPositionWithoutCreatingARow() {
        when(profiles.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        ProfileModels.ProfileView view = service.getProfile(owner, AS_OF);

        assertThat(view.totalIncome()).containsEntry("amount", "0.00");
        assertThat(view.availableCapacity()).containsEntry("amount", "0.00");
        assertThat(view.incomes()).isEmpty();
        assertThat(view.expenses()).isEmpty();
        verify(profiles, never()).save(any());
    }

    @Test
    void putProfileCreatesOrReplacesTheOwnersSingleProfile() {
        when(profiles.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(incomes.findByProfileIdAndOwnerId(any(), eq(ownerId))).thenReturn(List.of());
        when(expenses.findByProfileIdAndOwnerId(any(), eq(ownerId))).thenReturn(List.of());

        ProfileModels.ProfileView created = service.putProfile(owner,
                new ProfileModels.PutProfileCommand("VND", new BigDecimal("10.00"),
                        new BigDecimal("5.00"), 2), AS_OF);

        assertThat(created.savingsAmount()).isEqualTo("10.00");
        assertThat(created.dependentsCount()).isEqualTo(2);
        verify(profiles).save(any(ProfileEntity.class));

        ProfileEntity existing = profile();
        java.time.Instant before = existing.getUpdatedAt();
        when(profiles.findByOwnerId(ownerId)).thenReturn(Optional.of(existing));

        ProfileModels.ProfileView replaced = service.putProfile(owner,
                new ProfileModels.PutProfileCommand("VND", new BigDecimal("99.00"),
                        new BigDecimal("1.00"), 0), AS_OF);

        assertThat(replaced.savingsAmount()).isEqualTo("99.00");
        assertThat(replaced.emergencyFundAmount()).isEqualTo("1.00");
        assertThat(replaced.dependentsCount()).isZero();
        assertThat(existing.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void addingOrChangingALineWithoutATargetIsNotFound() {
        when(profiles.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addIncome(owner, new ProfileModels.IncomeCommand(
                new BigDecimal("1.00"), "salary", AS_OF)))
                .isInstanceOf(ResourceNotFoundException.class);

        when(incomes.findByIdAndOwnerId(any(), eq(ownerId))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateIncome(owner, UUID.randomUUID(),
                new ProfileModels.IncomeCommand(new BigDecimal("1.00"), "salary", AS_OF)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteIncome(owner, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        when(expenses.findByIdAndOwnerId(any(), eq(ownerId))).thenReturn(Optional.empty());
        when(expenses.deleteByIdAndOwnerId(any(), eq(ownerId))).thenReturn(0);
        assertThatThrownBy(() -> service.updateExpense(owner, UUID.randomUUID(),
                new ProfileModels.ExpenseCommand(new BigDecimal("1.00"), "rent", "FIXED", AS_OF)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteExpense(owner, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void everyOwnerScopedLookupPassesTheSessionOwnerIdNeverAClientValue() {
        stubProfileWithLines(List.of(income("74.00", "salary", true)), List.of());
        IncomeEntity line = income("74.00", "salary", true);
        UUID lineId = UUID.randomUUID();
        when(incomes.findByIdAndOwnerId(lineId, ownerId)).thenReturn(Optional.of(line));
        when(incomes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(incomes.deleteByIdAndOwnerId(lineId, ownerId)).thenReturn(1);

        service.getProfile(owner, AS_OF);
        service.updateIncome(owner, lineId, new ProfileModels.IncomeCommand(
                new BigDecimal("74.00"), "salary", AS_OF));
        service.deleteIncome(owner, lineId);

        verify(incomes).findByProfileIdAndOwnerId(any(), eq(ownerId));
        verify(incomes).findByIdAndOwnerId(lineId, ownerId);
        verify(incomes).deleteByIdAndOwnerId(lineId, ownerId);
    }

    @Test
    void theReturnedViewCarriesProvenanceAndNoOwnerOrPersistenceIdentifiers() {
        stubProfileWithLines(List.of(income("74.00", "salary", true)),
                List.of(expense("30.00", "rent", true)));

        ProfileModels.ProfileView view = service.getProfile(owner, AS_OF);

        assertThat(view.provenance()).extracting(ProfileModels.ProvenanceView::field)
                .containsExactly("Income", "Expense", "Net Cash Flow", "Available Capacity");
        assertThat(view.provenance()).allSatisfy(entry -> {
            assertThat(entry.kind()).isEqualTo("calculated");
            assertThat(entry.detail()).isNotBlank();
        });

        assertThat(ProfileModels.ProfileView.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .as("DTO never leaks owner or persistence identifiers")
                .doesNotContain("ownerId", "accountId", "profileId");
    }

    @Test
    void moneyStringsReachTheViewWithoutFloatingPointConversion() {
        stubProfileWithLines(
                List.of(income("19.99", "salary", true), income("0.01", "side", true)),
                List.of(expense("0.10", "rent", true), expense("0.20", "food", true)));

        Map<String, String> totals = service.getProfile(owner, AS_OF).totalIncome();

        assertThat(totals.get("amount")).isEqualTo("20.00");
        assertThat(totals.get("amount")).matches("\\d+\\.\\d{2}");
    }
}
