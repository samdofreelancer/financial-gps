package com.financialgps.platform.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T005 — platform-side boundary guard (plan §Boundary): no type under
 * {@code com.financialgps.domain.*} may reference Spring/JDBC/auth-platform/Clock/Random types.
 * The engine never receives identity — this test is the automated form of that promise.
 */
class DomainBoundaryGuardTest {

    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("\\borg\\.springframework\\."),
            Pattern.compile("\\bjakarta\\."),
            Pattern.compile("\\bjava\\.sql\\."),
            Pattern.compile("\\bjava\\.time\\.Clock\\b"),
            Pattern.compile("\\bjava\\.util\\.Random\\b"),
            Pattern.compile("\\bcom\\.financialgps\\.platform\\."),
            Pattern.compile("\\bcom\\.financialgps\\.application\\."),
            Pattern.compile("\\bcom\\.financialgps\\.infrastructure\\."),
            Pattern.compile("\\bcom\\.financialgps\\.api\\."));

    @Test
    void domainLaneReferencesNoPlatformOrFrameworkTypes() throws IOException {
        Path domainRoot = Path.of("src/main/java/com/financialgps/domain");
        assertThat(domainRoot).as("domain source folder exists").exists();

        try (Stream<Path> sources = Files.walk(domainRoot)) {
            List<Path> files = sources.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
            assertThat(files).as("domain source files present").isNotEmpty();

            for (Path file : files) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                for (Pattern forbidden : FORBIDDEN) {
                    assertThat(source).as(
                                    "%s must not reference %s (the engine never receives identity)",
                                    file, forbidden.pattern())
                            .doesNotContainPattern(forbidden);
                }
            }
        }
    }

    /** Removes block/line comments so prose in Javadoc cannot trip the reference check. */
    private static String stripComments(String source) {
        String withoutBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("//[^\n]*", "");
    }
}
