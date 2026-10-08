package com.financialgps.api.account;

import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.application.account.ReauthRequiredException;
import com.financialgps.application.account.port.in.AuthenticateOwner;
import com.financialgps.application.account.port.in.ChangePassword;
import com.financialgps.application.account.port.in.DeleteOwner;
import com.financialgps.application.account.port.in.ExportOwnerData;
import com.financialgps.application.account.port.in.GetAccount;
import com.financialgps.application.account.port.out.SessionInvalidationPort;
import com.financialgps.application.account.port.out.CurrentCaller;
import com.financialgps.platform.security.SessionAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Owner-scoped account endpoints: me / export / delete. Every method resolves the actor from the
 * session via {@link CurrentCaller} and passes only the {@code OwnerId} value into an input
 * port — no caller-supplied owner id can ever exist (FR-013, SC-008).
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final CurrentCaller currentOwnerProvider;
    private final GetAccount getAccount;
    private final ExportOwnerData exportOwnerData;
    private final DeleteOwner deleteOwner;
    private final ChangePassword changePassword;
    private final AuthenticateOwner authenticateOwner;
    private final SessionInvalidationPort sessionInvalidation;
    private final SessionAuthenticator sessionAuthenticator;
    private final ObjectMapper objectMapper;

    public AccountController(CurrentCaller currentOwnerProvider,
                             GetAccount getAccount,
                             ExportOwnerData exportOwnerData,
                             DeleteOwner deleteOwner,
                             ChangePassword changePassword,
                             AuthenticateOwner authenticateOwner,
                             SessionInvalidationPort sessionInvalidation,
                             SessionAuthenticator sessionAuthenticator,
                             ObjectMapper objectMapper) {
        this.currentOwnerProvider = currentOwnerProvider;
        this.getAccount = getAccount;
        this.exportOwnerData = exportOwnerData;
        this.deleteOwner = deleteOwner;
        this.changePassword = changePassword;
        this.authenticateOwner = authenticateOwner;
        this.sessionInvalidation = sessionInvalidation;
        this.sessionAuthenticator = sessionAuthenticator;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/me")
    public AccountView me() {
        return getAccount.me(currentOwnerProvider.requireCurrentOwner());
    }

    /**
     * Deterministic bundle as raw JSON bytes: the document shape is mapped here, not in the use case.
     *
     * <p>T3 re-authentication: GET carries no body, so the password is taken from the
     * {@code X-Reauth-Password} header. Absent → 400 VALIDATION_FAILED (the client omitted the
     * step); wrong → 401 INVALID_CREDENTIALS (same body as login — no hint which part failed).
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestHeader(name = "X-Reauth-Password", required = false) String reauthPassword) throws Exception {
        OwnerId owner = currentOwnerProvider.requireCurrentOwner();
        AccountView account = getAccount.me(owner);
        if (reauthPassword == null || reauthPassword.isBlank()) {
            throw new ReauthRequiredException(
                    "Re-authentication password is required (X-Reauth-Password header).");
        }
        authenticateOwner.authenticate(account.email(), reauthPassword);
        ExportBundle bundle = exportOwnerData.export(owner);
        Map<String, Object> document = ExportBundleJson.document(bundle);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsBytes(document));
    }

    /**
     * T2: change the owner's password. Session mechanics stay here, not in the use case: every
     * OTHER session row of the owner is killed, while the caller's own session survives — the
     * user is not signed out by their own security action.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               HttpServletRequest httpRequest) {
        OwnerId owner = currentOwnerProvider.requireCurrentOwner();
        changePassword.change(owner, request.currentPassword(), request.newPassword());
        String keepId = httpRequest.getSession(false) != null ? httpRequest.getSession(false).getId() : null;
        sessionInvalidation.invalidateOthers(owner, keepId);
        return ResponseEntity.noContent().build();
    }

    /** US5: confirmed, irreversible hard delete (export-then-delete is the UI flow, FR-012). */
    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        OwnerId owner = currentOwnerProvider.requireCurrentOwner();
        // T3 re-auth: the irreversible endpoint must prove the caller knows the password.
        AccountView account = getAccount.me(owner);
        authenticateOwner.authenticate(account.email(), request.password());
        deleteOwner.delete(owner, request.confirmation());
        // Session invalidation stays outside the use case: a servlet session is not a database
        // resource and must never be enlisted in the DB transaction (see DeleteOwnerUseCase).
        // T1 fix: kill EVERY session of the owner, not just the caller's.
        sessionInvalidation.invalidateAll(owner);
        sessionAuthenticator.clearSessionCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }
}
