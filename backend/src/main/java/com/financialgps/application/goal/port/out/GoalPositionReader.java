package com.financialgps.application.goal.port.out;

import com.financialgps.domain.model.OwnerId;
import java.time.LocalDate;

/**
 * Reads the Financial Position boundary: Available Capacity (canonical max(NetCashFlow, 0) from
 * CashFlowCalculator) plus the evaluation currency and asOf date. 003 never recomputes it.
 */
public interface GoalPositionReader {

    PositionSnapshot snapshot(OwnerId owner);

    record PositionSnapshot(String availableCapacityAmount, String currency, LocalDate asOf) {
    }
}
