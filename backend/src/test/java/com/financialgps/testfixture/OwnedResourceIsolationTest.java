package com.financialgps.testfixture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.register;
import static com.financialgps.testsupport.AuthFlows.session;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T013 — FR-010: a cross-owner id is indistinguishable from a missing id — one identical 404
 * RESOURCE_NOT_FOUND body, so resource ids cannot be probed.
 */
class OwnedResourceIsolationTest extends IntegrationTestBase {

    @Test
    void crossOwnerGetEqualsMissingResourceBody() throws Exception {
        String ownerASession = register(mockMvc, uniqueEmail(), PASSWORD);
        String resourceId = createResource(ownerASession);

        String ownerBSession = register(mockMvc, uniqueEmail(), PASSWORD);

        String crossOwnerBody = mockMvc.perform(get("/api/test/owned/" + resourceId)
                        .cookie(session(ownerBSession)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        String missingBody = mockMvc.perform(get("/api/test/owned/" + UUID.randomUUID())
                        .cookie(session(ownerBSession)))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // FR-010: cross-owner and missing are indistinguishable. The RFC 7807 `instance` field
        // legitimately carries the requested path, so equality is compared without it.
        assertThat(problemWithoutInstance(crossOwnerBody))
                .isEqualTo(problemWithoutInstance(missingBody))
                .containsEntry("code", "RESOURCE_NOT_FOUND");
    }

    private Map<String, Object> problemWithoutInstance(String body) throws Exception {
        Map<String, Object> problem = new ObjectMapper().readValue(body, new TypeReference<>() {
        });
        problem.remove("instance");
        return problem;
    }

    @Test
    void crossOwnerMutationsAreRejectedAndLeaveTheResourceUntouched() throws Exception {
        String ownerASession = register(mockMvc, uniqueEmail(), PASSWORD);
        String resourceId = createResource(ownerASession);

        String ownerBSession = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(AuthFlows.withCsrf(mockMvc, patch("/api/test/owned/" + resourceId))
                        .cookie(session(ownerBSession))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"tampered\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(AuthFlows.withCsrf(mockMvc, post("/api/test/owned/" + resourceId + "/archive"))
                        .cookie(session(ownerBSession)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/test/owned/" + resourceId).cookie(session(ownerASession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("a-resource"))
                .andExpect(jsonPath("$.archived").value(false));
    }

    @Test
    void listIsOwnerScoped() throws Exception {
        String ownerASession = register(mockMvc, uniqueEmail(), PASSWORD);
        createResource(ownerASession);
        createResource(ownerASession);

        String ownerBSession = register(mockMvc, uniqueEmail(), PASSWORD);

        mockMvc.perform(get("/api/test/owned").cookie(session(ownerBSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String createResource(String ownerSession) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/test/owned")
                .cookie(session(ownerSession))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"a-resource\"}");
        return mockMvc.perform(AuthFlows.withCsrf(mockMvc, request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }
}
