package com.financialgps.testfixture;

import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T014 — SC-003 authorization matrix: owner B performs GET/LIST/UPDATE/ARCHIVE/DELETE against
 * owner A's resource ids — zero cross-owner successes; A's data is unchanged; the unauthenticated
 * variants answer 401.
 */
class AuthorizationMatrixTest extends IntegrationTestBase {

    private int crossOwnerSuccesses = 0;

    @Test
    void noOperationFromAnotherOwnerSucceeds() throws Exception {
        String ownerASession = register(mockMvc, uniqueEmail(), PASSWORD);
        String resourceA1 = createResource(ownerASession, "a-1");
        String resourceA2 = createResource(ownerASession, "a-2");

        String ownerBSession = register(mockMvc, uniqueEmail(), PASSWORD);

        for (String id : new String[]{resourceA1, resourceA2}) {
            expectNotFound(ownerBSession, get("/api/test/owned/" + id));
            expectNotFound(ownerBSession, patch("/api/test/owned/" + id)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"hijacked\"}"));
            expectNotFound(ownerBSession, post("/api/test/owned/" + id + "/archive"));
            expectNotFound(ownerBSession, delete("/api/test/owned/" + id));
        }

        // LIST: B sees none of A's rows.
        String listBody = mockMvc.perform(get("/api/test/owned").cookie(session(ownerBSession)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        if (listBody.contains(resourceA1) || listBody.contains(resourceA2)) {
            crossOwnerSuccesses++;
        }

        // Unauthenticated variants.
        mockMvc.perform(get("/api/test/owned"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/test/owned")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"anon\"}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        // A's data untouched by every attempted operation.
        mockMvc.perform(get("/api/test/owned/" + resourceA1).cookie(session(ownerASession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("a-1"))
                .andExpect(jsonPath("$.archived").value(false));
        mockMvc.perform(get("/api/test/owned/" + resourceA2).cookie(session(ownerASession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("a-2"))
                .andExpect(jsonPath("$.archived").value(false));

        assertThat(crossOwnerSuccesses).as("SC-003: zero cross-owner successes").isZero();
    }

    private void expectNotFound(String session, MockHttpServletRequestBuilder request) throws Exception {
        MockHttpServletRequestBuilder withCsrf = AuthFlows.withCsrf(mockMvc, request);
        String body = mockMvc.perform(withCsrf.cookie(session(session)))
                .andReturn().getResponse().getContentAsString();
        int status = mockMvc.perform(withCsrf.cookie(session(session))).andReturn().getResponse().getStatus();
        if (status != 404) {
            crossOwnerSuccesses++;
        }
        assertThat(status).as("cross-owner %s must be 404", request).isEqualTo(404);
        assertThat(body).contains("RESOURCE_NOT_FOUND");
    }

    private String createResource(String ownerSession, String label) throws Exception {
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/test/owned")
                        .cookie(session(ownerSession))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"" + label + "\"}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }
}
