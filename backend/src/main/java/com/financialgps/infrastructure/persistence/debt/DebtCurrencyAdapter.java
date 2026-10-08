package com.financialgps.infrastructure.persistence.debt;

import com.financialgps.application.debt.port.out.DebtCurrency;
import com.financialgps.application.profile.port.out.ProfileRecord;
import com.financialgps.application.profile.port.out.ProfileStore;
import com.financialgps.domain.model.Money;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;

/**
 * Currency adapter: debts inherit the stored profile currency (spec 002 §7), defaulting
 * exactly like the position reader when the owner has no profile yet.
 */
@Component
class DebtCurrencyAdapter implements DebtCurrency {

    private final ProfileStore profiles;

    DebtCurrencyAdapter(ProfileStore profiles) {
        this.profiles = profiles;
    }

    @Override
    public String currencyOf(OwnerId owner) {
        return profiles.findByOwner(owner).map(ProfileRecord::currency)
                .orElse(Money.DEFAULT_CURRENCY);
    }
}
