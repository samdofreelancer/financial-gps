package com.financialgps.api.auth;

import com.financialgps.application.account.model.AccountView;
import com.financialgps.application.account.model.OwnerRole;
import com.financialgps.application.account.port.in.AuthenticateOwner;
import com.financialgps.application.account.port.in.RegisterOwner;
import com.financialgps.application.account.port.out.SessionInvalidationPort;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.platform.security.OwnerPrincipal;
import com.financialgps.platform.security.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication handshake endpoints (plan §api layer). HTTP shape only: bind/validate, delegate to
 * the input ports, sign the principal in over the session. No hashing, no session mechanics, no
 * persistence here.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterOwner registerOwner;
    private final AuthenticateOwner authenticateOwner;
    private final SessionAuthenticator sessionAuthenticator;
    private final SessionInvalidationPort sessionInvalidation;

    public AuthController(RegisterOwner registerOwner,
                          AuthenticateOwner authenticateOwner,
                          SessionAuthenticator sessionAuthenticator,
                          SessionInvalidationPort sessionInvalidation) {
        this.registerOwner = registerOwner;
        this.authenticateOwner = authenticateOwner;
        this.sessionAuthenticator = sessionAuthenticator;
        this.sessionInvalidation = sessionInvalidation;
    }

    /** US1: register → auto sign-in (201 + session cookie). */
    @PostMapping("/register")
    public ResponseEntity<AccountView> register(@Valid @RequestBody RegisterRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        AccountView account = registerOwner.register(request.email(), request.password());
        sessionAuthenticator.signIn(
                new OwnerPrincipal(account.id(), account.email(), OwnerRole.OWNER),
                httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/v1/account/me")
                .body(account);
    }

    /** US2: login → server-side session bound to the browser (session id rotated if one existed). */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        AccountView account = authenticateOwner.authenticate(request.email(), request.password());
        sessionAuthenticator.signIn(
                new OwnerPrincipal(account.id(), account.email(), OwnerRole.OWNER),
                httpRequest, httpResponse);
        return ResponseEntity.ok(new LoginResponse(account.id(), account.email()));
    }

    /**
     * T1: kill EVERY session of the current owner (all browsers / devices). The row backing the
     * caller's own cookie is deleted too, so this request also 401s the current cookie afterwards.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal OwnerPrincipal principal,
                                          HttpServletResponse httpResponse) {
        if (principal != null) {
            sessionInvalidation.invalidateAll(new OwnerId(principal.id()));
            sessionAuthenticator.clearSessionCookie(httpResponse);
        }
        return ResponseEntity.noContent().build();
    }

    /** US2: logout terminates the session server-side (FR-003). Idempotent. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                       @AuthenticationPrincipal OwnerPrincipal principal) {
        if (principal != null) {
            sessionAuthenticator.logout(httpRequest, httpResponse);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * CSRF warm-up (T002): anonymous, idempotent; the response seeds the {@code XSRF-TOKEN}
     * cookie that state-changing requests echo back as {@code X-XSRF-TOKEN}.
     */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfWarmUpResponse> csrf(CsrfToken token) {
        // Binding CsrfToken forces materialization; the eager request handler also writes the cookie.
        return ResponseEntity.ok(new CsrfWarmUpResponse(token.getHeaderName()));
    }

    public record CsrfWarmUpResponse(String headerName) {
    }
}
