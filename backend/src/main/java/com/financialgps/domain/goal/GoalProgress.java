package com.financialgps.domain.goal;

import com.financialgps.domain.model.Money;

import java.math.BigDecimal;

/** Pure progress projection: remaining, progress ratio (scale 4), evaluated status. */
public record GoalProgress(Money remaining, BigDecimal progress, GoalStatus status) {
}
