package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStatus;
import com.financialgps.domain.debt.DebtType;
import com.financialgps.domain.debt.Rate;
import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Port RECORD <-> domain Debt mapping (no formula here). */
final class DebtMapping {

    static final String CURRENCY = "VND";

    private DebtMapping() {
    }

    static DebtRecord toRecord(UUID id, OwnerId owner, DebtModels.DebtCommand command) {
        Debt debt = toDomain(command.creditor(), command.debtType(), command.originalPrincipal(),
                command.outstandingBalance(), command.annualInterestRate(), command.minimumPayment(),
                command.plannedPayment(), command.dueDay());
        boolean paidOff = debt.status() == DebtStatus.PAID_OFF;
        return new DebtRecord(id, owner, command.creditor(), command.debtType(),
                command.originalPrincipal(), command.outstandingBalance(), command.annualInterestRate(),
                paidOff ? "0.00" : command.minimumPayment(),
                paidOff ? "0.00" : command.plannedPayment(), command.dueDay(),
                debt.status().name());
    }

    static DebtRecord toRecord(UUID id, OwnerId owner, DebtModels.DebtUpdateCommand command) {
        return toRecord(id, owner, new DebtModels.DebtCommand(command.creditor(), command.debtType(),
                command.originalPrincipal(), command.outstandingBalance(), command.annualInterestRate(),
                command.minimumPayment(), command.plannedPayment(), command.dueDay()));
    }

    static Debt toDomain(DebtRecord record) {
        return new Debt(record.creditor(), DebtType.valueOf(record.debtType()),
                Money.of(record.originalPrincipal(), CURRENCY),
                Money.of(record.outstandingBalance(), CURRENCY),
                record.annualInterestRate() == null ? null : Rate.of(record.annualInterestRate()),
                Money.of(record.minimumPayment(), CURRENCY),
                Money.of(record.plannedPayment(), CURRENCY),
                record.dueDay(), DebtStatus.valueOf(record.status()));
    }

    static List<Debt> toDebts(List<DebtRecord> records) {
        List<Debt> debts = new ArrayList<>();
        for (DebtRecord record : records) {
            Debt debt = toDomain(record);
            if (debt.contributesToTotals()) {
                debts.add(debt);
            }
        }
        return List.copyOf(debts);
    }

    static Debt toDomain(String creditor, String debtType, String originalPrincipal,
                         String outstandingBalance, String annualInterestRate,
                         String minimumPayment, String plannedPayment, Integer dueDay) {
        try {
            boolean paidOff = new BigDecimal(outstandingBalance).compareTo(BigDecimal.ZERO) == 0;
            return new Debt(creditor, DebtType.valueOf(debtType),
                    Money.of(originalPrincipal, CURRENCY), Money.of(outstandingBalance, CURRENCY),
                    annualInterestRate == null ? null : Rate.of(annualInterestRate),
                    Money.of(paidOff ? "0.00" : minimumPayment, CURRENCY),
                    Money.of(paidOff ? "0.00" : plannedPayment, CURRENCY),
                    dueDay, paidOff ? DebtStatus.PAID_OFF : DebtStatus.ACTIVE);
        } catch (DomainValidationException e) {
            throw new DebtValidationException(e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DebtValidationException("DEBT_INVALID_TYPE", "Unknown debt type: " + debtType);
        }
    }
}
