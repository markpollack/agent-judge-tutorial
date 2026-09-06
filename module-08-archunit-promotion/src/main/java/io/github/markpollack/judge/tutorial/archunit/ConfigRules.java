package io.github.markpollack.judge.tutorial.archunit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import io.github.markpollack.judge.result.Check;

/**
 * Deterministic, and not ArchUnit's business.
 *
 * <p>Two of the thirteen rules are as mechanisable as anything here and have nothing to do with
 * bytecode. "Flyway must be the only schema creator" is a fact about properties and migration
 * directories. "The Maven workflow must own the database matrix" is a fact about a YAML file.
 * Both are exact, both run in microseconds, and reaching for ArchUnit because ArchUnit is the
 * architecture tool would produce nothing.
 *
 * <p>The useful question is never "can ArchUnit check this". It is "what is the least
 * interpretive instrument that can answer it", and for these two the answer is
 * {@code Files.readString} and a substring.
 */
public final class ConfigRules {

    private ConfigRules() {
    }

    /** RULE-9: Flyway owns the schema, and nothing else creates one. */
    public static Check flywayOwnsTheSchema(Path workspace) {
        String properties = read(workspace.resolve("src/main/resources/application.properties"));
        boolean enabled = properties.contains("spring.flyway.enabled=true");
        boolean noAutoBaseline = properties.contains("spring.flyway.baseline-on-migrate=false");
        boolean noSchemaSql = !Files.exists(workspace.resolve("src/main/resources/schema.sql"));
        List<String> vendors = List.of("h2", "mysql", "postgres");
        boolean migrations = vendors.stream()
            .allMatch(vendor -> Files.isDirectory(workspace.resolve("src/main/resources/db/migration/" + vendor)));

        if (enabled && noAutoBaseline && noSchemaSql && migrations) {
            return Check.pass("RULE-9", "Flyway enabled, automatic baseline off, no schema.sql, "
                + vendors.size() + " vendor migration paths present");
        }
        return Check.fail("RULE-9", "flyway.enabled=" + enabled + ", baseline-on-migrate off=" + noAutoBaseline
            + ", schema.sql absent=" + noSchemaSql + ", vendor migrations present=" + migrations);
    }

    /** RULE-11: the Maven workflow owns the single database matrix, and Gradle does not. */
    public static Check mavenOwnsTheDatabaseMatrix(Path workspace) {
        String workflow = read(workspace.resolve(".github/workflows/maven-build.yml")).toLowerCase(Locale.ROOT);
        List<String> required = List.of("mysql", "postgres");
        List<String> missing = required.stream().filter(vendor -> !workflow.contains(vendor)).toList();

        if (missing.isEmpty()) {
            return Check.pass("RULE-11", "the Maven workflow exercises " + required.size() + " database vendors");
        }
        return Check.fail("RULE-11", "the Maven workflow is required to own the single "
            + "H2/MySQL/PostgreSQL migration-and-concurrency matrix and never mentions "
            + String.join(" or ", missing));
    }

    private static String read(Path path) {
        try {
            return Files.exists(path) ? Files.readString(path) : "";
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
