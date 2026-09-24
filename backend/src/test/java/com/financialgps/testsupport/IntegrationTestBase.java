package com.financialgps.testsupport;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers PostgreSQL base for full-flow tests (plan §Testing strategy). One
 * container per JVM; the Spring context is cached across test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Tests run plain HTTP through MockMvc; Secure cookies are asserted via header text.
        registry.add("financial.auth.cookie.secure", () -> "false");
        // Add the test-only owned-resource fixture tables used by ownership/cascade tests.
        registry.add("spring.flyway.locations",
                () -> "classpath:db/migration,classpath:db/test-migration");
    }

    @Autowired
    protected MockMvc mockMvc;
}
