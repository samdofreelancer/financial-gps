package com.financialgps.application.goal.port.in;

import com.financialgps.domain.model.OwnerId;
import java.util.UUID;

public interface DeleteGoal {
    void delete(OwnerId owner, UUID id);
}
