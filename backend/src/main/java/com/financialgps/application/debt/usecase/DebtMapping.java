package com.financialgps.application.debt.usecase;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.debt.model.DebtModels;
import com.financialgps.application.debt.port.out.DebtRecord;
import com.financialgps.domain.debt.Debt;
import com.financialgps.domain.debt.DebtStatus;
import com.financialgps.domain.model.DomainValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Port RECORD <-> domain Debt mapping (no formula here, and no value-object construction either:
 * {@code Money}/{@code Rate} are built by the domain reconstitution factories so a mapper can never
 * invent a monetary value, e.g. turn an unknown original principal into 0.00).
 */
final class DebtMapping {

    /** Single-currency MVP: debts share the profile currency; VND is the default (spec §7/§10.3). */
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
                debt.status().name(), null);
    }

    static DebtRecord toRecord(UUID id, OwnerId owner, DebtModels.DebtUpdateCommand command) {
        return toRecord(id, owner, new DebtModels.DebtCommand(command.creditor(), command.debtType(),
                command.originalPrincipal(), command.outstandingBalance(), command.annualInterestRate(),
                command.minimumPayment(), command.plannedPayment(), command.dueDay()));
    }

    static Debt toDomain(DebtRecord record) {
        return Debt.reconstitute(CURRENCY, record.creditor(), record.debtType(),
                record.originalPrincipal(), record.outstandingBalance(), record.annualInterestRate(),
                record.minimumPayment(), record.plannedPayment(), record.dueDay(),
                DebtStatus.valueOf(record.status()));
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
            return Debt.recorded(CURRENCY, creditor, debtType, originalPrincipal, outstandingBalance,
                    annualInterestRate, minimumPayment, plannedPayment, dueDay);
        } catch (DomainValidationException e) {
            throw new DebtValidationException(e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DebtValidationException("DEBT_INVALID_TYPE", "Unknown debt type: " + debtType);
        }
    }
}
