package com.financialgps.api.auth;

import com.financialgps.application.account.RegistrationConflictException;
import com.financialgps.application.account.port.in.RegisterOwner;
import com.financialgps.testsupport.AuthFlows;
import com.financialgps.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.financialgps.testsupport.AuthFlows.PASSWORD;
import static com.financialgps.testsupport.AuthFlows.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pre-check ({@code existsByLowerEmail}) can never be trusted under concurrency: two requests
 * both see "free" and both insert. The DB unique index ({@code ux_account_email_lower}) is the real
 * arbiter, so the losing transaction must still answer the documented conflict contract
 * ({@code 409 REGISTRATION_FAILED}) instead of leaking a raw persistence error as a 500.
 *
 * <p>BCrypt hashing (strength 12) sits between the pre-check and the insert, which makes the
 * interleaving realistic rather than contrived: both threads clear the pre-check before either
 * commits.
 */
class ConcurrentRegistrationTest extends IntegrationTestBase {

    @Autowired
    private RegisterOwner registerOwner;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void simultaneousDuplicateRegistrationsLeaveOneAccountAndOneConflict() throws Exception {
        String email = uniqueEmail();
        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            List<Callable<Throwable>> attempts = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                attempts.add(() -> {
                    startLine.await(30, TimeUnit.SECONDS);
                    try {
                        registerOwner.register(email, PASSWORD);
                        return null; // this request won the insert
                    } catch (Throwable failure) {
                        return failure;
                    }
                });
            }
            List<Future<Throwable>> futures = attempts.stream().map(pool::submit).toList();
            startLine.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                outcomes.add(future.get(120, TimeUnit.SECONDS));
            }

            long winners = outcomes.stream().filter(Objects::isNull).count();
            List<Throwable> losers = outcomes.stream().filter(Objects::nonNull).toList();

            assertThat(winners).as("exactly one concurrent registration may create the account").isEqualTo(1);
            assertThat(losers).hasSize(1);
            assertThat(losers.get(0))
                    .as("the loser must get the documented conflict contract, never a raw DB error")
                    .isInstanceOf(RegistrationConflictException.class);
            assertThat(jdbc.queryForObject(
                    "select count(*) from account where lower(email) = lower(?)", Long.class, email))
                    .as("the unique index must have kept a single account row")
                    .isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * The same race at the HTTP boundary — the layer the SPA actually talks to. Nothing here may
     * ever answer 5xx: the loser has to receive {@code 409 REGISTRATION_FAILED} with the generic
     * body (FR-004), because "registration is not available" is indistinguishable from
     * "that email is taken".
     */
    @Test
    void concurrentDuplicateRegistrationsAnswer201And409AtTheApiBoundary() throws Exception {
        String email = uniqueEmail();
        List<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> requests =
                new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            // Warm up each request's own CSRF handshake before the gate opens, so the two
            // registrations really do collide.
            requests.add(AuthFlows.withCsrf(mockMvc, AuthFlows.register(email, PASSWORD)));
        }

        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<MvcResult>> attempts = new ArrayList<>();
            for (var request : requests) {
                attempts.add(() -> {
                    startLine.await(30, TimeUnit.SECONDS);
                    return mockMvc.perform(request).andReturn();
                });
            }
            List<Future<MvcResult>> futures = attempts.stream().map(pool::submit).toList();
            startLine.countDown();

            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(120, TimeUnit.SECONDS));
            }

            List<Integer> statuses = results.stream()
                    .map(result -> result.getResponse().getStatus())
                    .sorted()
                    .collect(Collectors.toList());
            assertThat(statuses)
                    .as("one registration wins with 201, the other is rejected with 409 — never 5xx")
                    .containsExactly(201, 409);

            MvcResult rejected = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 409)
                    .findFirst()
                    .orElseThrow();
            assertThat(rejected.getResponse().getContentAsString())
                    .contains("\"code\":\"REGISTRATION_FAILED\"")
                    .doesNotContainIgnoringCase("duplicate")
                    .doesNotContainIgnoringCase("ux_account_email_lower");
            assertThat(jdbc.queryForObject(
                    "select count(*) from account where lower(email) = lower(?)", Long.class, email))
                    .isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }
}
