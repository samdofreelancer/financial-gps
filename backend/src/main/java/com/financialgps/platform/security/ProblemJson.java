package com.financialgps.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared RFC 7807 body shape for problems written outside the MVC advice (security entry point
 * and access-denied handler). Field order matches the advice bodies so error contracts are
 * uniform (plan §Error catalogue, SC-005).
 */
public final class ProblemJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProblemJson() {
    }

    public static Map<String, Object> body(int status, String code, String title, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status);
        body.put("code", code);
        body.put("detail", detail);
        return body;
    }

    public static void write(jakarta.servlet.http.HttpServletResponse response, int status, String code,
                             String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(), body(status, code, title, detail));
    }
}
