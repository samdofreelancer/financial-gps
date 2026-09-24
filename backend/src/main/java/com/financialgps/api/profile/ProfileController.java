package com.financialgps.api.profile;

import com.financialgps.application.account.OwnerId;
import com.financialgps.application.profile.ProfileModels;
import com.financialgps.application.profile.ProfileService;
import com.financialgps.platform.security.CurrentOwnerProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** HTTP adapter only: binds/validates DTOs, delegates to ProfileService. No math. */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService service;
    private final CurrentOwnerProvider owners;

    public ProfileController(ProfileService service, CurrentOwnerProvider owners) {
        this.service = service;
        this.owners = owners;
    }

    @GetMapping
    public ProfileModels.ProfileView get() {
        OwnerId owner = owners.requireCurrentOwner();
        return service.getProfile(owner, null);
    }

    @PutMapping
    public ProfileModels.ProfileView put(@Valid @RequestBody ProfileDtos.PutProfileRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return service.putProfile(owner, new ProfileModels.PutProfileCommand(
                request.currency(), new BigDecimal(request.savingsAmount()),
                new BigDecimal(request.emergencyFundAmount()), request.dependentsCount()), null);
    }
}
