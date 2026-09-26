package com.financialgps.api.profile;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.AddIncome;
import com.financialgps.application.profile.port.in.DeleteIncome;
import com.financialgps.application.profile.port.in.UpdateIncome;
import com.financialgps.platform.security.CurrentOwnerProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP adapter only for income lines: bind → validate → resolve actor → invoke input port. The
 * effective date is a business-date concern owned by the use case, never by this adapter.
 */
@RestController
@RequestMapping("/api/v1/incomes")
public class IncomeController {

    private final AddIncome addIncome;
    private final UpdateIncome updateIncome;
    private final DeleteIncome deleteIncome;
    private final CurrentOwnerProvider owners;

    public IncomeController(AddIncome addIncome, UpdateIncome updateIncome, DeleteIncome deleteIncome,
                            CurrentOwnerProvider owners) {
        this.addIncome = addIncome;
        this.updateIncome = updateIncome;
        this.deleteIncome = deleteIncome;
        this.owners = owners;
    }

    @PostMapping
    public ResponseEntity<ProfileModels.IncomeView> create(@Valid @RequestBody ProfileDtos.IncomeRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        ProfileModels.IncomeView view = addIncome.add(owner,
                new ProfileModels.IncomeCommand(request.amount(), request.source()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/{id}")
    public ProfileModels.IncomeView update(@PathVariable UUID id,
                                           @Valid @RequestBody ProfileDtos.IncomeRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return updateIncome.update(owner, id,
                new ProfileModels.IncomeCommand(request.amount(), request.source()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteIncome.delete(owners.requireCurrentOwner(), id);
        return ResponseEntity.noContent().build();
    }
}
