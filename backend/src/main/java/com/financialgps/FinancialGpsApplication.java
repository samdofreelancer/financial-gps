package com.financialgps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Financial GPS backend.
 *
 * <p>Package lanes (spec 007 plan §Components): {@code com.financialgps.domain} is the pure
 * Financial Domain Engine and must never depend on any platform/auth/JDBC/Spring type — this is
 * enforced by {@code DomainBoundaryGuardTest}. All platform capability (authentication,
 * authorization, ownership) lives under {@code api}, {@code application}, {@code platform},
 * {@code infrastructure}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FinancialGpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialGpsApplication.class, args);
    }
}
