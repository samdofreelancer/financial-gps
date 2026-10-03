package com.financialgps.application.debt.port.out;

import com.financialgps.application.account.model.OwnerId;
import java.time.LocalDate;

/** Technical-time port for the debt context (mirrors the profile BusinessDate port). */
public interface DebtBusinessDate {

    LocalDate today();
}
