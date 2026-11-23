package com.afterfall;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the architecture boundary: the plain Java simulation core
 * (domain and simulation packages) must not import framework or persistence classes.
 *
 * <p>Runs from the backend module directory (Maven Surefire's default working directory).
 */
class CoreBoundaryTest {

    private static final List<Path> CORE_DIRS = List.of(
            Path.of("src/main/java/com/afterfall/domain"),
            Path.of("src/main/java/com/afterfall/behavior"),
            Path.of("src/main/java/com/afterfall/action"),
            Path.of("src/main/java/com/afterfall/simulation"),
            Path.of("src/main/java/com/afterfall/observer"),
            Path.of("src/main/java/com/afterfall/events"),
            Path.of("src/main/java/com/afterfall/narrator"));

    private static final List<String> FORBIDDEN_PREFIXES = List.of(
            "import org.springframework",
            "import jakarta.persistence",
            "import javax.persistence",
            "import org.hibernate",
            "import org.postgresql");

    @Test
    void coreSourcesHaveNoFrameworkImports() throws IOException {
        for (Path dir : CORE_DIRS) {
            assertTrue(Files.isDirectory(dir), "Missing core package directory: " + dir);
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    for (String line : Files.readAllLines(file)) {
                        String trimmed = line.trim();
                        for (String forbidden : FORBIDDEN_PREFIXES) {
                            assertFalse(trimmed.startsWith(forbidden),
                                    file + " violates core boundary: " + trimmed);
                        }
                    }
                }
            }
        }
    }
}
