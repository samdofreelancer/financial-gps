package com.financialgps.api.profile;

import com.financialgps.application.account.OwnerId;
import com.financialgps.application.profile.ProfileModels;
import com.financialgps.application.profile.ProfileService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** HTTP adapter only for expense lines. */
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ProfileService service;
    private final CurrentOwnerProvider owners;

    public ExpenseController(ProfileService service, CurrentOwnerProvider owners) {
        this.service = service;
        this.owners = owners;
    }

    @PostMapping
    public ResponseEntity<ProfileModels.ExpenseView> create(@Valid @RequestBody ProfileDtos.ExpenseRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        ProfileModels.ExpenseView view = service.addExpense(owner, new ProfileModels.ExpenseCommand(
                new BigDecimal(request.amount()), request.category(), request.expenseType(), LocalDate.now()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/{id}")
    public ProfileModels.ExpenseView update(@PathVariable UUID id,
                                            @Valid @RequestBody ProfileDtos.ExpenseRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return service.updateExpense(owner, id, new ProfileModels.ExpenseCommand(
                new BigDecimal(request.amount()), request.category(), request.expenseType(), LocalDate.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteExpense(owners.requireCurrentOwner(), id);
        return ResponseEntity.noContent().build();
    }
}
