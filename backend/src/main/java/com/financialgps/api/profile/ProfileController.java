package com.financialgps.api.profile;

import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.profile.model.ProfileModels;
import com.financialgps.application.profile.port.in.GetProfile;
import com.financialgps.application.profile.port.in.PutProfile;
import com.financialgps.platform.security.CurrentOwnerProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter only: binds/validates DTOs, resolves the authenticated actor, invokes input ports.
 * No financial rule, no parsing, no clock — the business date belongs to the {@code BusinessDate}
 * output port (plan Phase 3 steps 1–3).
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final GetProfile getProfile;
    private final PutProfile putProfile;
    private final CurrentOwnerProvider owners;

    public ProfileController(GetProfile getProfile, PutProfile putProfile, CurrentOwnerProvider owners) {
        this.getProfile = getProfile;
        this.putProfile = putProfile;
        this.owners = owners;
    }

    @GetMapping
    public ProfileModels.ProfileView get() {
        OwnerId owner = owners.requireCurrentOwner();
        return getProfile.get(owner);
    }

    @PutMapping
    public ProfileModels.ProfileView put(@Valid @RequestBody ProfileDtos.PutProfileRequest request) {
        OwnerId owner = owners.requireCurrentOwner();
        return putProfile.put(owner, new ProfileModels.PutProfileCommand(
                request.currency(), request.savingsAmount(), request.emergencyFundAmount(),
                request.dependentsCount()));
    }
}
