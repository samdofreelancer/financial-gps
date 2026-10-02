package com.financialgps.application.debt.port.out;

import com.financialgps.application.account.model.OwnerId;
import java.util.UUID;

/**
 * Persistence-shaped debt facts exchanged with a {@link DebtStore} adapter.
 * Amounts travel as plain decimal strings; rate is nullable (missing ≠ 0%).
 * {@code id} is {@code null} for a create and the stored row id for an update.
 */
public record DebtRecord(UUID id, OwnerId owner, String creditor, String debtType,
                         String originalPrincipal, String outstandingBalance,
                         String annualInterestRate, String minimumPayment, String plannedPayment,
                         Integer dueDay, String status) {
}
