package com.financialgps.api.account;

import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T016 — export endpoint: complete machine-readable bundle with all sections (empty arrays until
 * financial features register exporters), byte-identical across consecutive exports (SC-006).
 */
class ExportEndpointTest extends IntegrationTestBase {

    @Test
    void exportBundleIsCompleteAndDeterministic() throws Exception {
        String email = uniqueEmail();
        String sessionId = register(mockMvc, email, PASSWORD);

        String firstExport = mockMvc.perform(get("/api/v1/account/export").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formatVersion").value(1))
                .andExpect(jsonPath("$.account.email").value(email))
                .andExpect(jsonPath("$.profile").isArray())
                .andExpect(jsonPath("$.incomes").isArray())
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.debts").isArray())
                .andExpect(jsonPath("$.goals").isArray())
                .andExpect(jsonPath("$.timelineChanges").isArray())
                .andExpect(jsonPath("$.allocationRules").isArray())
                .andExpect(jsonPath("$.gpsSnapshots").isArray())
                .andExpect(jsonPath("$.reviewLedger").isArray())
                .andReturn().getResponse().getContentAsString();

        String secondExport = mockMvc.perform(get("/api/v1/account/export").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(firstExport).isEqualTo(secondExport);
        assertThat(firstExport).doesNotContain("password");
    }

    @Test
    void unauthenticatedExportIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/account/export"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }
}
