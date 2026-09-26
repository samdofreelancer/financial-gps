package com.financialgps.api.profile;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.AddExpense;
import com.financialgps.application.profile.port.in.DeleteExpense;
import com.financialgps.application.profile.port.in.UpdateExpense;
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
 * HTTP adapter only for expense lines: bind → validate → resolve actor → invoke input port. The
 * effective date is a business-date concern owned by the use case, never by this adapter.
 */
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final AddExpense addExpense;
    private final UpdateExpense updateExpense;
    private final DeleteExpense deleteExpense;
    private final CurrentOwnerProvider owners;

    public ExpenseController(AddExpense addExpense, UpdateExpense updateExpense,
                             DeleteExpense deleteExpense, CurrentOwnerProvider owners) {
        this.addExpense = addExpense;
        this.updateExpense = updateExpense;
        this.deleteExpense = deleteExpense;
        this.owners = owners;
    }

    @PostMapping
    public ResponseEntity<ProfileModels.ExpenseView> create(@Valid @RequestBody ProfileDtos.ExpenseRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        ProfileModels.ExpenseView view = addExpense.add(owner, new ProfileModels.ExpenseCommand(
                request.amount(), request.category(), request.expenseType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/{id}")
    public ProfileModels.ExpenseView update(@PathVariable UUID id,
                                            @Valid @RequestBody ProfileDtos.ExpenseRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return updateExpense.update(owner, id, new ProfileModels.ExpenseCommand(
                request.amount(), request.category(), request.expenseType()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteExpense.delete(owners.requireCurrentOwner(), id);
        return ResponseEntity.noContent().build();
    }
}
