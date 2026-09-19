package com.financialgps.api.auth;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.login;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T010 — anti-enumeration: login failures are byte-identical for unknown email vs wrong
 * password; duplicate-email registration never hints at existence (FR-004, research §6).
 */
class AntiRevelationTest extends IntegrationTestBase {

    @Test
    void loginFailuresAreByteIdentical() throws Exception {
        String email = uniqueEmail();
        register(mockMvc, email, PASSWORD);

        String unknownEmailBody = mockMvc.perform(
                        AuthFlows.withCsrf(mockMvc, login("ghost-" + uniqueEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String wrongPasswordBody = mockMvc.perform(
                        AuthFlows.withCsrf(mockMvc, login(email, "wrong password 99")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmailBody).isEqualTo(wrongPasswordBody);
        assertThat(unknownEmailBody).contains("INVALID_CREDENTIALS");
    }

    @Test
    void duplicateRegistrationBodyContainsNoExistenceHint() throws Exception {
        String email = uniqueEmail();
        register(mockMvc, email, PASSWORD);

        String firstDuplicate = mockMvc.perform(
                        AuthFlows.withCsrf(mockMvc, AuthFlows.register(email, PASSWORD)))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        String caseVariantDuplicate = mockMvc.perform(
                        AuthFlows.withCsrf(mockMvc, AuthFlows.register(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(firstDuplicate).isEqualTo(caseVariantDuplicate);
        String lowered = firstDuplicate.toLowerCase();
        assertThat(lowered).doesNotContain("taken").doesNotContain("exist").doesNotContain("already");
    }
}
