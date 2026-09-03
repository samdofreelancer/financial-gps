package com.financialgps.api.auth;

import com.financialgps.application.account.AccountView;
import com.financialgps.application.account.AuthenticateOwnerService;
import com.financialgps.application.account.PasswordPolicyViolationException;
import com.financialgps.application.account.RegistrationConflictException;
import com.financialgps.application.account.RegisterOwnerService;
import com.financialgps.api.common.ProblemDetailAdvice;
import com.financialgps.platform.security.SecurityConfig;
import com.financialgps.platform.security.SessionAuthenticator;
import com.financialgps.testsupport.AuthFlows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T007 — registration endpoint contract: 201 on success, 422 with requirement list (FR-005),
 * generic 409 with no existence hint (FR-004), 400 VALIDATION_FAILED for malformed DTOs.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, ProblemDetailAdvice.class})
class RegistrationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterOwnerService registerOwnerService;

    @MockBean
    private AuthenticateOwnerService authenticateOwnerService;

    @MockBean
    private SessionAuthenticator sessionAuthenticator;

    @Test
    void validRegistrationReturns201WithAccountView() throws Exception {
        UUID id = UUID.randomUUID();
        when(registerOwnerService.register(anyString(), anyString()))
                .thenReturn(new AccountView(id, "user@example.com", Instant.parse("2026-08-25T10:00:00Z")));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, AuthFlows.register("user@example.com", "correct horse battery1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-25T10:00:00Z"));
    }

    @Test
    void weakPasswordReturns422ListingRequirements() throws Exception {
        when(registerOwnerService.register(anyString(), anyString()))
                .thenThrow(new PasswordPolicyViolationException(List.of(
                        "Password must be at least 10 characters long.",
                        "Password must contain at least one digit.")));

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, AuthFlows.register("user@example.com", "short")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PASSWORD_POLICY_VIOLATION"))
                .andExpect(jsonPath("$.violations.length()").value(2));
    }

    @Test
    void duplicateEmailReturnsGeneric409WithoutExistenceHint() throws Exception {
        when(registerOwnerService.register(anyString(), anyString()))
                .thenThrow(new RegistrationConflictException());

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, AuthFlows.register("user@example.com", "correct horse battery1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_FAILED"))
                .andExpect(jsonPath("$.detail")
                        .value("Registration failed. Try again with different details."));
    }

    @Test
    void malformedEmailReturns400ValidationFailed() throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, AuthFlows.register("not-an-email", "correct horse battery1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void missingBodyFieldsReturns400ValidationFailed() throws Exception {
        mockMvc.perform(AuthFlows.withCsrf(mockMvc,
                        AuthFlows.register("user@example.com", "correct horse battery1").content("{}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
