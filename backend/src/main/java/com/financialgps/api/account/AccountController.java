package com.financialgps.api.account;

import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.ExportBundle;
import com.financialgps.application.account.model.OwnerId;
import com.financialgps.application.account.port.in.DeleteOwner;
import com.financialgps.application.account.port.in.ExportOwnerData;
import com.financialgps.application.account.port.in.GetAccount;
import com.financialgps.platform.security.CurrentOwnerProvider;
import com.financialgps.platform.security.SessionAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Owner-scoped account endpoints: me / export / delete. Every method resolves the actor from the
 * session via {@link CurrentOwnerProvider} and passes only the {@code OwnerId} value into an input
 * port — no caller-supplied owner id can ever exist (FR-013, SC-008).
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final CurrentOwnerProvider currentOwnerProvider;
    private final GetAccount getAccount;
    private final ExportOwnerData exportOwnerData;
    private final DeleteOwner deleteOwner;
    private final SessionAuthenticator sessionAuthenticator;
    private final ObjectMapper objectMapper;

    public AccountController(CurrentOwnerProvider currentOwnerProvider,
                             GetAccount getAccount,
                             ExportOwnerData exportOwnerData,
                             DeleteOwner deleteOwner,
                             SessionAuthenticator sessionAuthenticator,
                             ObjectMapper objectMapper) {
        this.currentOwnerProvider = currentOwnerProvider;
        this.getAccount = getAccount;
        this.exportOwnerData = exportOwnerData;
        this.deleteOwner = deleteOwner;
        this.sessionAuthenticator = sessionAuthenticator;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/me")
    public AccountView me() {
        return getAccount.me(currentOwnerProvider.requireCurrentOwner());
    }

    /** Deterministic bundle as raw JSON bytes: the document shape is mapped here, not in the use case. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws Exception {
        ExportBundle bundle = exportOwnerData.export(currentOwnerProvider.requireCurrentOwner());
        Map<String, Object> document = ExportBundleJson.document(bundle);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsBytes(document));
    }

    /** US5: confirmed, irreversible hard delete (export-then-delete is the UI flow, FR-012). */
    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        OwnerId owner = currentOwnerProvider.requireCurrentOwner();
        deleteOwner.delete(owner, request.confirmation());
        // Session invalidation stays outside the use case: a servlet session is not a database
        // resource and must never be enlisted in the DB transaction (see DeleteOwnerUseCase).
        sessionAuthenticator.logout(httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }
}
