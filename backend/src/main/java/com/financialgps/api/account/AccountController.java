package com.financialgps.api.account;

import com.financialgps.application.account.AccountReader;
import com.financialgps.application.account.AccountView;
import com.financialgps.application.account.DataExportService;
import com.financialgps.application.account.DeleteAccountService;
import com.financialgps.application.account.OwnerId;
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
 * Owner-scoped account endpoints: me / export / delete (plan §api layer). Every method resolves
 * the owner from the session via {@link CurrentOwnerProvider} — no caller-supplied owner id can
 * ever exist (FR-013, SC-008).
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final CurrentOwnerProvider currentOwnerProvider;
    private final AccountReader accountReader;
    private final DataExportService dataExportService;
    private final DeleteAccountService deleteAccountService;
    private final SessionAuthenticator sessionAuthenticator;
    private final ObjectMapper objectMapper;

    public AccountController(CurrentOwnerProvider currentOwnerProvider,
                             AccountReader accountReader,
                             DataExportService dataExportService,
                             DeleteAccountService deleteAccountService,
                             SessionAuthenticator sessionAuthenticator,
                             ObjectMapper objectMapper) {
        this.currentOwnerProvider = currentOwnerProvider;
        this.accountReader = accountReader;
        this.dataExportService = dataExportService;
        this.deleteAccountService = deleteAccountService;
        this.sessionAuthenticator = sessionAuthenticator;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/me")
    public AccountView me() {
        return accountReader.me(currentOwnerProvider.requireCurrentOwner());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() throws Exception {
        Map<String, Object> bundle = dataExportService.export(currentOwnerProvider.requireCurrentOwner());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsBytes(bundle));
    }

    /** US5: confirmed, irreversible hard delete (export-then-delete is the UI flow, FR-012). */
    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        OwnerId owner = currentOwnerProvider.requireCurrentOwner();
        deleteAccountService.deleteAccount(owner, request.confirmation());
        sessionAuthenticator.logout(httpRequest, httpResponse); // invalidate the deleted account's session
        return ResponseEntity.noContent().build();
    }
}
