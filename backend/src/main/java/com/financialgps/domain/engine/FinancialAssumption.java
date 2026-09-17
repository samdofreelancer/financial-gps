package com.financialgps.domain.engine;

/** One labelled assumption: USER_SUPPLIED or SYSTEM_DEFAULT. */
public record FinancialAssumption(String name, String value, String source) {
}
