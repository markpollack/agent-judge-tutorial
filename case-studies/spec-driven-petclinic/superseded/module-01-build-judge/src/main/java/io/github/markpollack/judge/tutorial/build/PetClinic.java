package io.github.markpollack.judge.tutorial.build;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;

/**
 * The subject every module in this tutorial evaluates.
 *
 * <p>An agent was given one task against Spring PetClinic:
 *
 * <blockquote>Add a way to find owners by their city. Follow the conventions already used in
 * the codebase.</blockquote>
 *
 * <p>The candidate workspace is materialized from a pinned baseline plus a frozen patch, so
 * every run of this tutorial evaluates byte-identical code. See
 * {@code fixtures/petclinic/PROVENANCE.md}.
 */
public final class PetClinic {

    /** The task the agent was given. Judges are told this, not the answer. */
    public static final String GOAL =
        "Add a way to find owners by their city. Follow the conventions already used in the codebase.";

    private static final Path FIXTURES = Path.of("fixtures/petclinic");

    private PetClinic() {
    }

    /**
     * The workspace holding the agent's change, materialized on first use.
     *
     * <p>Deterministic and offline: a copy of the pinned baseline with
     * {@code city-search.patch} applied. Nothing is fetched.
     */
    public static Path candidateWorkspace() {
        Path workspace = FIXTURES.resolve("build/city-search-candidate");
        if (!Files.isDirectory(workspace)) {
            materialize();
        }
        return workspace;
    }

    /**
     * The large spec-driven candidate, materialized so it builds.
     *
     * <p>The vendored fixture does not build: it fails spring-javaformat on one file, which is a
     * finding recorded in PROVENANCE.md rather than repaired in place. The script derives a
     * buildable copy and reports what it had to change.
     */
    public static Path largeCandidate() {
        Path workspace = FIXTURES.resolve("build/large-candidate");
        if (!Files.isDirectory(workspace)) {
            System.out.println("Materializing the large candidate from the pinned fixture...");
            run(FIXTURES, "./materialize-large-candidate.sh");
        }
        return workspace;
    }

    /**
     * The same candidate with one seeded defect, for module 06's negative control.
     *
     * <p>It differs from {@link #largeCandidate()} by a single comparison operator, on a boundary
     * the 290-test suite does not test. The suite stays green and one acceptance criterion is no
     * longer met, which is the only condition under which a spec-conformance judge earns its keep.
     */
    public static Path seededCandidate() {
        Path workspace = FIXTURES.resolve("build/seeded-candidate");
        if (!Files.isDirectory(workspace)) {
            System.out.println("Materializing the seeded candidate from the pinned fixture...");
            run(FIXTURES, "./materialize-seeded-candidate.sh");
        }
        return workspace;
    }

    /** The unchanged code the agent started from. */
    public static Path baselineWorkspace() {
        return FIXTURES.resolve("baseline");
    }

    private static void materialize() {
        System.out.println("Materializing the candidate workspace from the pinned baseline...");
        run(FIXTURES, "./materialize-city-search.sh");
    }

    private static void run(Path directory, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
            process.getInputStream().transferTo(OutputStream.nullOutputStream());
            if (!process.waitFor(15, TimeUnit.MINUTES) || process.exitValue() != 0) {
                throw new IllegalStateException(String.join(" ", command) + " failed in " + directory);
            }
        }
        catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Could not run " + String.join(" ", command), e);
        }
    }

    /**
     * Line coverage of the pinned baseline, measured, not assumed.
     *
     * <p>296 of 314 lines, from {@code ./mvnw -o test jacoco:report} on
     * {@code fixtures/petclinic/baseline} at commit {@code 88e37c15}. Reproduce it with the
     * command in {@code fixtures/petclinic/evidence/README.md}.
     */
    public static final double BASELINE_LINE_COVERAGE = 94.27;

    /** The JaCoCo report path {@code JaCoCoReportParser} reads, relative to a workspace. */
    public static final String COVERAGE_REPORT = "target/site/jacoco/jacoco.xml";

    /**
     * Guarantee a coverage report exists in the candidate workspace.
     *
     * <p>Module 01 leaves one behind. If module 02 is run on its own, the build has to happen
     * here instead, because a measurement with no evidence behind it is not a measurement.
     */
    public static Path candidateWithCoverage() {
        Path workspace = candidateWorkspace();
        if (!Files.isRegularFile(workspace.resolve(COVERAGE_REPORT))) {
            System.out.println("No coverage report yet. Producing the evidence first...");
            run(workspace, "./mvnw", "-o", "-q", "test", "jacoco:report");
        }
        return workspace;
    }

    /** A context describing what the agent was asked to do and where its work landed. */
    public static JudgmentContext contextFor(Path workspace) {
        return contextBuilder(workspace).build();
    }

    /** The same context, still open, for modules that add their own metadata. */
    public static JudgmentContext.Builder contextBuilder(Path workspace) {
        return JudgmentContext.builder()
            .goal(GOAL)
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(4));
    }
}
