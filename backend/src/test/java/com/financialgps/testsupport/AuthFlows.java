package com.financialgps.testsupport;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared full-flow helpers. State-changing requests go through the REAL double-submit handshake:
 * warm up {@code XSRF-TOKEN} via {@code GET /auth/csrf}, then echo it back as the
 * {@code X-XSRF-TOKEN} header together with the cookie (research §4, T002).
 */
public final class AuthFlows {

    public static final String PASSWORD = "correct horse battery1";

    private AuthFlows() {
    }

    public static String uniqueEmail() {
        return "owner-" + UUID.randomUUID() + "@example.com";
    }

    /** Adds the CSRF handshake (cookie + header) to any state-changing request builder. */
    public static MockHttpServletRequestBuilder withCsrf(MockMvc mockMvc, MockHttpServletRequestBuilder builder)
            throws Exception {
        MvcResult warmUp = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie xsrfCookie = warmUp.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).as("warm-up must seed the XSRF-TOKEN cookie").isNotNull();
        return builder.cookie(xsrfCookie).header("X-XSRF-TOKEN", xsrfCookie.getValue());
    }

    public static MockHttpServletRequestBuilder register(String email, String password) {
        return post("/api/v1/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    public static MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    /** Registers and returns the signed-in SESSION cookie value. */
    public static String register(MockMvc mockMvc, String email, String password) throws Exception {
        return mockMvc.perform(withCsrf(mockMvc, register(email, password)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn()
                .getResponse()
                .getCookie("SESSION")
                .getValue();
    }

    public static jakarta.servlet.http.Cookie session(String value) {
        return new jakarta.servlet.http.Cookie("SESSION", value);
    }
}
