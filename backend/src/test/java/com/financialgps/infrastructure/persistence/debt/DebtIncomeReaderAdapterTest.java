package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.profile.port.out.BusinessDate;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import com.financialgps.domain.model.OwnerId;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The DTI denominator applies the domain-owned effective-date rule
 * ({@code Income.isEffective}): future-dated and inactive lines never inflate it.
 */
class DebtIncomeReaderAdapterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final OwnerId OWNER = new OwnerId(UUID.randomUUID());

    private static IncomeRecord row(String amount, boolean active, LocalDate effectiveFrom) {
        return new IncomeRecord(UUID.randomUUID(), OWNER, UUID.randomUUID(), amount, "salary",
                active, effectiveFrom);
    }

    private DebtIncomeReaderAdapter adapter(List<IncomeRecord> rows) {
        IncomeStore incomes = mock(IncomeStore.class);
        when(incomes.findAllByOwner(OWNER)).thenReturn(rows);
        BusinessDate dates = mock(BusinessDate.class);
        when(dates.today()).thenReturn(TODAY);
        return new DebtIncomeReaderAdapter(incomes, dates);
    }

    @Test
    void sumsOnlyActiveLinesEffectiveToday() {
        DebtIncomeReaderAdapter adapter = adapter(List.of(
                row("100.00", true, TODAY),
                row("50.00", true, TODAY.minusDays(1)),
                row("999.00", true, TODAY.plusDays(1)),
                row("999.00", false, TODAY)));

        Optional<BigDecimal> total = adapter.totalActiveMonthlyIncome(OWNER);

        assertThat(total).hasValueSatisfying(v ->
                assertThat(v).isEqualByComparingTo(new BigDecimal("150.00")));
    }

    @Test
    void emptyWhenNoIncomeOrZeroTotal() {
        assertThat(adapter(List.of()).totalActiveMonthlyIncome(OWNER)).isEmpty();
        assertThat(adapter(List.of(row("0.00", true, TODAY)))
                .totalActiveMonthlyIncome(OWNER)).isEmpty();
    }
}
