package io.github.markpollack.judge.tutorial.build;

import java.io.IOException;
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

    /** The unchanged code the agent started from. */
    public static Path baselineWorkspace() {
        return FIXTURES.resolve("baseline");
    }

    private static void materialize() {
        try {
            System.out.println("Materializing the candidate workspace from the pinned baseline...");
            Process process = new ProcessBuilder("./materialize-city-search.sh")
                .directory(FIXTURES.toFile())
                .redirectErrorStream(true)
                .start();
            if (!process.waitFor(5, TimeUnit.MINUTES) || process.exitValue() != 0) {
                throw new IllegalStateException("materialize-city-search.sh failed");
            }
        }
        catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Could not materialize the candidate workspace", e);
        }
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
