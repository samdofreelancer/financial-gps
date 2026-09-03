package com.financialgps.api.auth;

import com.financialgps.application.account.AccountView;
import com.financialgps.application.account.AuthenticateOwnerService;
import com.financialgps.application.account.RegisterOwnerService;
import com.financialgps.platform.security.OwnerPrincipal;
import com.financialgps.platform.security.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
 * Authentication handshake endpoints (plan §api layer). HTTP shape only: bind/validate, delegate
 * to the application services, sign the principal in over the session. No hashing or session
 * mechanics here.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterOwnerService registerOwnerService;
    private final AuthenticateOwnerService authenticateOwnerService;
    private final SessionAuthenticator sessionAuthenticator;

    public AuthController(RegisterOwnerService registerOwnerService,
                          AuthenticateOwnerService authenticateOwnerService,
                          SessionAuthenticator sessionAuthenticator) {
        this.registerOwnerService = registerOwnerService;
        this.authenticateOwnerService = authenticateOwnerService;
        this.sessionAuthenticator = sessionAuthenticator;
    }

    /** US1: register → auto sign-in (201 + session cookie). */
    @PostMapping("/register")
    public ResponseEntity<AccountView> register(@Valid @RequestBody RegisterRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        AccountView account = registerOwnerService.register(request.email(), request.password());
        sessionAuthenticator.signIn(
                new OwnerPrincipal(account.id(), account.email(), RegisterOwnerService.OWNER_ROLE),
                httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    /** US2: login → server-side session bound to the browser (session id rotated if one existed). */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        AccountView account = authenticateOwnerService.authenticate(request.email(), request.password());
        sessionAuthenticator.signIn(
                new OwnerPrincipal(account.id(), account.email(), RegisterOwnerService.OWNER_ROLE),
                httpRequest, httpResponse);
        return ResponseEntity.ok(new LoginResponse(account.id(), account.email()));
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
