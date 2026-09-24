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

/** HTTP adapter only for income lines. */
@RestController
@RequestMapping("/api/v1/incomes")
public class IncomeController {

    private final ProfileService service;
    private final CurrentOwnerProvider owners;

    public IncomeController(ProfileService service, CurrentOwnerProvider owners) {
        this.service = service;
        this.owners = owners;
    }

    @PostMapping
    public ResponseEntity<ProfileModels.IncomeView> create(@Valid @RequestBody ProfileDtos.IncomeRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        ProfileModels.IncomeView view = service.addIncome(owner, new ProfileModels.IncomeCommand(
                new BigDecimal(request.amount()), request.source(), LocalDate.now()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/{id}")
    public ProfileModels.IncomeView update(@PathVariable UUID id,
                                           @Valid @RequestBody ProfileDtos.IncomeRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return service.updateIncome(owner, id, new ProfileModels.IncomeCommand(
                new BigDecimal(request.amount()), request.source(), LocalDate.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteIncome(owners.requireCurrentOwner(), id);
        return ResponseEntity.noContent().build();
    }
}
