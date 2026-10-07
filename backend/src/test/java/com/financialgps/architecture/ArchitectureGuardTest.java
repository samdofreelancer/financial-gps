package com.financialgps.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 of {@code specs/010-ddd-refactor/plan.md}: package-level dependency rules for the
 * PRODUCTION sources ({@code src/main/java}) in this single-module Maven project.
 *
 * <p>These rules are the automated form of the intended dependency direction — they must FAIL when
 * an application use case imports infrastructure, when an HTTP adapter reaches a repository, when a
 * financial formula leaves the domain, or when a new port is wired without a transaction boundary.
 * The domain-purity rule itself stays in {@code DomainBoundaryGuardTest} (the name the constitution
 * and README reference).
 */
class ArchitectureGuardTest {

    private static final Path MAIN = Path.of("src/main/java");
    private static final Path LANE = MAIN.resolve("com/financialgps");
    private static final Path CONFIGURATION = LANE.resolve("infrastructure/configuration");

    /** The only non-domain class allowed to call the canonical engine / build domain Money. */
    private static final String SANCTIONED_ENGINE_CALLER =
            "application/profile/usecase/ProfileAssembler.java";

    /** The export bundle reads only and was never transactional, so it stays unwrapped by design. */
    private static final String NON_TRANSACTIONAL_INPUT_PORT = "ExportOwnerData";

    @Test
    void applicationLaneImportsNoFrameworkInfrastructureApiOrPlatformType() throws IOException {
        List<Pattern> forbidden = List.of(
                Pattern.compile("\\borg\\.springframework\\."),
                Pattern.compile("\\bjakarta\\."),
                Pattern.compile("\\bjava\\.sql\\."),
                Pattern.compile("\\bcom\\.financialgps\\.infrastructure\\."),
                Pattern.compile("\\bcom\\.financialgps\\.api\\."),
                Pattern.compile("\\bcom\\.financialgps\\.platform\\."));

        for (Path file : sourcesUnder(LANE.resolve("application"))) {
            String source = code(file);
            for (Pattern pattern : forbidden) {
                assertThat(source).as("%s must not reference %s", relative(file), pattern.pattern())
                        .doesNotContainPattern(pattern);
            }
        }
    }

    @Test
    void applicationPortsAreFreeOfEveryFrameworkType() throws IOException {
        for (Path file : sourcesUnder(LANE.resolve("application/account/port"))) {
            String source = code(file);
            assertThat(source).as("%s: a port is a pure contract", relative(file))
                    .doesNotContain("springframework")
                    .doesNotContain("jakarta")
                    .doesNotContain("infrastructure");
        }
        for (Path file : sourcesUnder(LANE.resolve("application/profile/port"))) {
            String source = code(file);
            assertThat(source).as("%s: a port is a pure contract", relative(file))
                    .doesNotContain("springframework")
                    .doesNotContain("jakarta")
                    .doesNotContain("infrastructure");
        }
    }

    @Test
    void apiLaneNeverReachesInfrastructure() throws IOException {
        for (Path file : sourcesUnder(LANE.resolve("api"))) {
            String source = code(file);
            assertThat(source).as("%s must not reference %s", relative(file), "infrastructure")
                    .doesNotContain("com.financialgps.infrastructure");
            assertThat(source).as("%s: an HTTP adapter owns no persistence type", relative(file))
                    .doesNotContain("JpaRepository")
                    .doesNotContain("jakarta.persistence")
                    .doesNotContain("org.springframework.data")
                    .doesNotContainPattern(Pattern.compile("\\b\\w+Repository\\b"))
                    .doesNotContainPattern(
                            Pattern.compile("\\b(?!ResponseEntity|HttpEntity|RequestEntity)\\w+Entity\\b"));
        }
    }

    @Test
    void jpaEntitiesAndSpringDataRepositoriesLiveOnlyInThePersistenceLane() throws IOException {
        for (Path file : sourcesUnder(LANE)) {
            String lane = relative(file);
            if (lane.startsWith("infrastructure/persistence/")) {
                continue;
            }
            String source = code(file);
            assertThat(source).as("%s must not declare a JPA entity", lane).doesNotContain("@Entity");
            assertThat(source).as("%s must not declare a Spring Data repository", lane)
                    .doesNotContain("extends JpaRepository");
        }
    }

    @Test
    void financialRulesStayInTheDomainLane() throws IOException {
        List<Pattern> domainOnly = List.of(
                Pattern.compile("\\bFinancialEngine\\b"),
                Pattern.compile("\\bCashFlowCalculator\\b"),
                Pattern.compile("\\bFinancialPolicy\\b"),
                Pattern.compile("\\bMoney\\.(of|zero)\\b"));

        for (Path file : sourcesUnder(LANE)) {
            String lane = relative(file);
            if (lane.startsWith("domain/") || lane.equals(SANCTIONED_ENGINE_CALLER)) {
                continue;
            }
            String source = code(file);
            for (Pattern pattern : domainOnly) {
                assertThat(source)
                        .as("%s: financial rules belong to the domain (%s)", lane, pattern.pattern())
                        .doesNotContainPattern(pattern);
            }
        }
    }

    @Test
    void apiAndApplicationNeverResolveTheBusinessDateThemselves() throws IOException {
        List<Path> lanes = new ArrayList<>(sourcesUnder(LANE.resolve("api")));
        lanes.addAll(sourcesUnder(LANE.resolve("application")));

        for (Path file : lanes) {
            assertThat(code(file))
                    .as("%s: the business date comes from the BusinessDate port", relative(file))
                    .doesNotContain("LocalDate.now()")
                    .doesNotContain("java.time.Clock");
        }
    }

    @Test
    void everyInputPortIsWiredAndTransactionDemarcated() throws IOException {
        List<String> configuration = new ArrayList<>();
        for (Path file : sourcesUnder(CONFIGURATION)) {
            configuration.add(code(file));
        }
        String wiring = String.join("\n", configuration);

        for (String port : simpleNamesOf("port/in")) {
            int declaration = wiring.indexOf("\n    " + port + " ");
            assertThat(declaration)
                    .as("%s must be exposed as a @Bean by infrastructure.configuration", port)
                    .isGreaterThanOrEqualTo(0);
            if (port.equals(NON_TRANSACTIONAL_INPUT_PORT)) {
                continue;
            }
            String window = wiring.substring(declaration,
                    Math.min(wiring.length(), declaration + 600));
            assertThat(window)
                    .as("%s must be transaction-demarcated at the input-port boundary", port)
                    .containsAnyOf("transactions.readOnly(", "transactions.writable(");
        }
    }

    @Test
    void everyOutputPortHasAnInfrastructureAdapter() throws IOException {
        List<String> infrastructure = new ArrayList<>();
        for (Path file : sourcesUnder(LANE.resolve("infrastructure"))) {
            infrastructure.add(code(file));
        }
        String adapters = String.join("\n", infrastructure);

        for (String port : simpleNamesOf("port/out")) {
            assertThat(adapters)
                    .as("%s must have at least one infrastructure adapter", port)
                    .contains("implements " + port);
        }
    }

    /** Simple names of the interfaces declared under any context's {@code port/<side>} folder. */
    private static List<String> simpleNamesOf(String portSide) throws IOException {
        List<String> names = new ArrayList<>();
        for (Path file : sourcesUnder(LANE.resolve("application"))) {
            if (!relative(file).contains("/" + portSide + "/")) {
                continue;
            }
            Matcher matcher = Pattern.compile("public interface (\\w+)").matcher(code(file));
            if (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        assertThat(names).as("ports declared under %s", portSide).isNotEmpty();
        return names;
    }

    /** Source text with comments removed, so prose cannot trip a reference rule. */
    private static String code(Path file) throws IOException {
        String withoutBlockComments = Files.readString(file, StandardCharsets.UTF_8)
                .replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("//[^\\n]*", "");
    }

    private static List<Path> sourcesUnder(Path root) throws IOException {
        return filesUnder(root).stream().filter(path -> path.toString().endsWith(".java")).toList();
    }

    private static List<Path> filesUnder(Path root) throws IOException {
        assertThat(root).as("%s exists", root).exists();
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }

    private static String relative(Path file) {
        return LANE.relativize(file).toString().replace('\\', '/');
    }
}
