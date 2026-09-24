package com.financialgps.infrastructure.persistence.profile;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.port.out.IncomeRecord;
import com.financialgps.application.profile.port.out.IncomeStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter for {@link IncomeStore}. Every query keeps an explicit owner predicate — the
 * owner-scoped repository convention (fixture convention / FR-007) is preserved verbatim, so no
 * adapter method can leak another owner's rows.
 */
@Component
class JpaIncomeStore implements IncomeStore {

    private final IncomeRepository incomes;

    JpaIncomeStore(IncomeRepository incomes) {
        this.incomes = incomes;
    }

    @Override
    public List<IncomeRecord> findAllByOwner(OwnerId owner) {
        return incomes.findByOwnerIdOrderByCreatedAt(owner.value()).stream()
                .map(JpaIncomeStore::toRecord)
                .toList();
    }

    @Override
    public List<IncomeRecord> findAllByProfile(UUID profileId, OwnerId owner) {
        return incomes.findByProfileIdAndOwnerId(profileId, owner.value()).stream()
                .map(JpaIncomeStore::toRecord)
                .toList();
    }

    @Override
    public Optional<IncomeRecord> findById(UUID id, OwnerId owner) {
        return incomes.findByIdAndOwnerId(id, owner.value()).map(JpaIncomeStore::toRecord);
    }

    @Override
    public IncomeRecord save(IncomeRecord income) {
        IncomeEntity entity;
        if (income.id() == null) {
            entity = new IncomeEntity(income.owner().value(), income.profileId(),
                    new BigDecimal(income.amount()), income.source(), income.active(),
                    income.effectiveFrom());
        } else {
            entity = incomes.findByIdAndOwnerId(income.id(), income.owner().value())
                    .orElseThrow(() -> new IllegalStateException(
                            "Income line " + income.id() + " vanished between read and write"));
            entity.setAmount(new BigDecimal(income.amount()));
            entity.setSource(income.source());
            entity.touch();
        }
        return toRecord(incomes.save(entity));
    }

    @Override
    public boolean delete(UUID id, OwnerId owner) {
        return incomes.deleteByIdAndOwnerId(id, owner.value()) > 0;
    }

    private static IncomeRecord toRecord(IncomeEntity entity) {
        return new IncomeRecord(entity.getId(), new OwnerId(entity.getOwnerId()),
                entity.getProfileId(), entity.getAmount().toPlainString(), entity.getSource(),
                entity.isActive(), entity.getEffectiveFrom());
    }
}
