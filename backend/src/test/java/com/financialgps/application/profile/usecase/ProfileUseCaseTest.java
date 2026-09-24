package com.financialgps.application.profile.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.ExpenseRecord;
import com.financialgps.application.profile.port.out.ExpenseStore;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
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
 * 001 §A1 — application orchestration only: owner-scoped loading, mapping to the canonical engine,
 * business-date resolution, and not-found semantics. Every collaborator is an OUTPUT PORT mock, so
 * the use cases run with no Spring context, no JPA and no database (plan Phase 1 exit gate).
 */
class ProfileUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());
    private static final UUID PROFILE_ID = UUID.randomUUID();

    /** The plan's "business date is explicit and controllable in tests" requirement, as a lambda. */
    private static final BusinessDate FIXED_DATE = () -> TODAY;

    private final ProfileStore profiles = mock(ProfileStore.class);
    private final IncomeStore incomes = mock(IncomeStore.class);
    private final ExpenseStore expenses = mock(ExpenseStore.class);

    private final GetProfileUseCase getProfile =
            new GetProfileUseCase(profiles, incomes, expenses, FIXED_DATE);
    private final PutProfileUseCase putProfile =
            new PutProfileUseCase(profiles, incomes, expenses, FIXED_DATE);
    private final AddIncomeUseCase addIncome = new AddIncomeUseCase(profiles, incomes, FIXED_DATE);
    private final UpdateIncomeUseCase updateIncome = new UpdateIncomeUseCase(incomes);
    private final DeleteIncomeUseCase deleteIncome = new DeleteIncomeUseCase(incomes);
    private final AddExpenseUseCase addExpense = new AddExpenseUseCase(profiles, expenses, FIXED_DATE);
    private final UpdateExpenseUseCase updateExpense = new UpdateExpenseUseCase(expenses);
    private final DeleteExpenseUseCase deleteExpense = new DeleteExpenseUseCase(expenses);

    private static ProfileRecord storedProfile() {
        return new ProfileRecord(PROFILE_ID, OWNER, "VND", "10.00", "5.00", 2);
    }

    private static IncomeRecord storedIncome(UUID id, String amount) {
        return new IncomeRecord(id, OWNER, PROFILE_ID, amount, "salary", true, TODAY.minusDays(30));
    }

    private static ExpenseRecord storedExpense(UUID id, String amount) {
        return new ExpenseRecord(id, OWNER, PROFILE_ID, amount, "rent", "FIXED", true,
                TODAY.minusDays(30));
    }

    @Test
    void getProfileUsesTheBusinessDatePortNotTheWallClock() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.of(storedProfile()));
        when(incomes.findAllByProfile(PROFILE_ID, OWNER)).thenReturn(List.of());
        when(expenses.findAllByProfile(PROFILE_ID, OWNER)).thenReturn(List.of());

        assertThat(getProfile.get(OWNER).asOf()).isEqualTo(TODAY.toString());
    }

    @Test
    void getProfileWithoutAProfileNeverQueriesTheLineStores() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.empty());

        ProfileModels.ProfileView view = getProfile.get(OWNER);

        verify(incomes, never()).findAllByProfile(any(), any());
        verify(expenses, never()).findAllByProfile(any(), any());
        assertThat(view.savingsAmount()).isEqualTo("0.00");
        assertThat(view.totalIncome().amount()).isEqualTo("0.00");
    }

    @Test
    void getProfileMapsStoredFactsThroughTheCanonicalEngine() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.of(storedProfile()));
        when(incomes.findAllByProfile(PROFILE_ID, OWNER))
                .thenReturn(List.of(storedIncome(UUID.randomUUID(), "74.00")));
        when(expenses.findAllByProfile(PROFILE_ID, OWNER))
                .thenReturn(List.of(storedExpense(UUID.randomUUID(), "30.00")));

        ProfileModels.ProfileView view = getProfile.get(OWNER);

        assertThat(view.currency()).isEqualTo("VND");
        assertThat(view.savingsAmount()).isEqualTo("10.00");
        assertThat(view.emergencyFundAmount()).isEqualTo("5.00");
        assertThat(view.dependentsCount()).isEqualTo(2);
        assertThat(view.totalIncome().amount()).isEqualTo("74.00");
        assertThat(view.totalExpenses().amount()).isEqualTo("30.00");
        assertThat(view.netCashFlow().amount()).isEqualTo("44.00");
        assertThat(view.availableCapacity().amount()).isEqualTo("44.00");
    }

    @Test
    void putProfileCreatesWhenAbsentAndReplacesWhenPresent() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.empty());
        when(profiles.save(any())).thenReturn(storedProfile());
        when(incomes.findAllByProfile(PROFILE_ID, OWNER)).thenReturn(List.of());
        when(expenses.findAllByProfile(PROFILE_ID, OWNER)).thenReturn(List.of());

        ProfileModels.ProfileView created = putProfile.put(OWNER,
                new ProfileModels.PutProfileCommand("VND", "10.00", "5.00", 2));

        ArgumentCaptor<ProfileRecord> createCaptor = ArgumentCaptor.forClass(ProfileRecord.class);
        verify(profiles).save(createCaptor.capture());
        assertThat(createCaptor.getValue().id()).as("a create has no id yet").isNull();
        assertThat(createCaptor.getValue().owner()).isEqualTo(OWNER);
        assertThat(created.savingsAmount()).isEqualTo("10.00");

        when(profiles.findByOwner(OWNER)).thenReturn(Optional.of(storedProfile()));
        putProfile.put(OWNER, new ProfileModels.PutProfileCommand("USD", "99.00", "1.00", 0));

        ArgumentCaptor<ProfileRecord> replaceCaptor = ArgumentCaptor.forClass(ProfileRecord.class);
        verify(profiles, org.mockito.Mockito.times(2)).save(replaceCaptor.capture());
        ProfileRecord replacement = replaceCaptor.getAllValues().get(1);
        assertThat(replacement.id()).as("a replace targets the existing row").isEqualTo(PROFILE_ID);
        assertThat(replacement.currency()).isEqualTo("USD");
        assertThat(replacement.dependentsCount()).isZero();
    }

    @Test
    void addIncomeRequiresAnExistingProfile() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addIncome.add(OWNER,
                new ProfileModels.IncomeCommand("1.00", "salary")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(incomes, never()).save(any());
    }

    @Test
    void addIncomeStampsTheBusinessDateAsTheEffectiveDateAndActivateFlag() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.of(storedProfile()));
        UUID assigned = UUID.randomUUID();
        when(incomes.save(any())).thenReturn(storedIncome(assigned, "74.00"));

        ProfileModels.IncomeView view = addIncome.add(OWNER,
                new ProfileModels.IncomeCommand("74.00", "salary"));

        ArgumentCaptor<IncomeRecord> captor = ArgumentCaptor.forClass(IncomeRecord.class);
        verify(incomes).save(captor.capture());
        IncomeRecord written = captor.getValue();
        assertThat(written.id()).isNull();
        assertThat(written.owner()).isEqualTo(OWNER);
        assertThat(written.profileId()).isEqualTo(PROFILE_ID);
        assertThat(written.effectiveFrom()).isEqualTo(TODAY);
        assertThat(written.active()).isTrue();
        assertThat(view.id()).isEqualTo(assigned);
        assertThat(view.amount()).isEqualTo("74.00");
        assertThat(view.source()).isEqualTo("salary");
    }

    @Test
    void updateIncomeReplacesEditableFieldsAndPreservesIdentityAndDates() {
        UUID lineId = UUID.randomUUID();
        when(incomes.findById(lineId, OWNER)).thenReturn(Optional.of(storedIncome(lineId, "74.00")));
        when(incomes.save(any())).thenReturn(storedIncome(lineId, "94.00"));

        ProfileModels.IncomeView view = updateIncome.update(OWNER, lineId,
                new ProfileModels.IncomeCommand("94.00", "salary"));

        ArgumentCaptor<IncomeRecord> captor = ArgumentCaptor.forClass(IncomeRecord.class);
        verify(incomes).save(captor.capture());
        IncomeRecord written = captor.getValue();
        assertThat(written.id()).isEqualTo(lineId);
        assertThat(written.profileId()).as("an edit never re-homes a line").isEqualTo(PROFILE_ID);
        assertThat(written.active()).isTrue();
        assertThat(written.effectiveFrom()).as("an edit never back-dates or re-dates a line")
                .isEqualTo(TODAY.minusDays(30));
        assertThat(written.amount()).isEqualTo("94.00");
        assertThat(view.amount()).isEqualTo("94.00");
    }

    @Test
    void missingOrCrossOwnerIncomeLinesAreIndistinguishableFromNotFound() {
        when(incomes.findById(any(), eq(OWNER))).thenReturn(Optional.empty());
        when(incomes.delete(any(), eq(OWNER))).thenReturn(false);

        assertThatThrownBy(() -> updateIncome.update(OWNER, UUID.randomUUID(),
                new ProfileModels.IncomeCommand("1.00", "salary")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> deleteIncome.delete(OWNER, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addExpenseRequiresAnExistingProfileAndStampsTheBusinessDate() {
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> addExpense.add(OWNER,
                new ProfileModels.ExpenseCommand("1.00", "rent", "FIXED")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(expenses, never()).save(any());

        when(profiles.findByOwner(OWNER)).thenReturn(Optional.of(storedProfile()));
        UUID assigned = UUID.randomUUID();
        when(expenses.save(any())).thenReturn(storedExpense(assigned, "30.00"));

        ProfileModels.ExpenseView view = addExpense.add(OWNER,
                new ProfileModels.ExpenseCommand("30.00", "rent", "FIXED"));

        ArgumentCaptor<ExpenseRecord> captor = ArgumentCaptor.forClass(ExpenseRecord.class);
        verify(expenses).save(captor.capture());
        ExpenseRecord written = captor.getValue();
        assertThat(written.id()).isNull();
        assertThat(written.profileId()).isEqualTo(PROFILE_ID);
        assertThat(written.effectiveFrom()).isEqualTo(TODAY);
        assertThat(written.active()).isTrue();
        assertThat(written.expenseType()).isEqualTo("FIXED");
        assertThat(view.id()).isEqualTo(assigned);
        assertThat(view.category()).isEqualTo("rent");
    }

    @Test
    void updateExpenseReplacesEditableFieldsAndPreservesIdentityAndDates() {
        UUID lineId = UUID.randomUUID();
        when(expenses.findById(lineId, OWNER)).thenReturn(Optional.of(storedExpense(lineId, "30.00")));
        when(expenses.save(any())).thenReturn(new ExpenseRecord(lineId, OWNER, PROFILE_ID, "35.00",
                "food", "VARIABLE", true, TODAY.minusDays(30)));

        ProfileModels.ExpenseView view = updateExpense.update(OWNER, lineId,
                new ProfileModels.ExpenseCommand("35.00", "food", "VARIABLE"));

        ArgumentCaptor<ExpenseRecord> captor = ArgumentCaptor.forClass(ExpenseRecord.class);
        verify(expenses).save(captor.capture());
        ExpenseRecord written = captor.getValue();
        assertThat(written.id()).isEqualTo(lineId);
        assertThat(written.profileId()).isEqualTo(PROFILE_ID);
        assertThat(written.effectiveFrom()).isEqualTo(TODAY.minusDays(30));
        assertThat(view.category()).isEqualTo("food");
        assertThat(view.expenseType()).isEqualTo("VARIABLE");
    }

    @Test
    void missingOrCrossOwnerExpenseLinesAreIndistinguishableFromNotFound() {
        when(expenses.findById(any(), eq(OWNER))).thenReturn(Optional.empty());
        when(expenses.delete(any(), eq(OWNER))).thenReturn(false);

        assertThatThrownBy(() -> updateExpense.update(OWNER, UUID.randomUUID(),
                new ProfileModels.ExpenseCommand("1.00", "rent", "FIXED")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> deleteExpense.delete(OWNER, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void everyOwnerScopedCallPassesTheSessionOwnerIdNeverAClientValue() {
        UUID lineId = UUID.randomUUID();
        when(profiles.findByOwner(OWNER)).thenReturn(Optional.of(storedProfile()));
        when(incomes.findAllByProfile(PROFILE_ID, OWNER)).thenReturn(List.of());
        when(expenses.findAllByProfile(PROFILE_ID, OWNER)).thenReturn(List.of());
        when(incomes.findById(lineId, OWNER)).thenReturn(Optional.of(storedIncome(lineId, "1.00")));
        when(incomes.save(any())).thenReturn(storedIncome(lineId, "1.00"));
        when(incomes.delete(lineId, OWNER)).thenReturn(true);

        getProfile.get(OWNER);
        updateIncome.update(OWNER, lineId, new ProfileModels.IncomeCommand("1.00", "salary"));
        deleteIncome.delete(OWNER, lineId);

        verify(profiles).findByOwner(OWNER);
        verify(incomes).findAllByProfile(PROFILE_ID, OWNER);
        verify(incomes).findById(lineId, OWNER);
        verify(incomes).delete(lineId, OWNER);
    }
}
