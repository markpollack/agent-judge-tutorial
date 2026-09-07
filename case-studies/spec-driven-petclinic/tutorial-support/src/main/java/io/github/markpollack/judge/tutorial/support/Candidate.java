package io.github.markpollack.judge.tutorial.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;

/**
 * The subject every module in this tutorial evaluates.
 *
 * <p>An agent was given a written specification and produced a working Spring application: 166
 * main Java files and 290 tests. It says it is done.
 *
 * <p>The specification is not ours. It ships inside the branch, was written before the code, and
 * carries 438 numbered requirements across fifteen documents. This tutorial reads two of them and
 * asks the implementation to answer for itself.
 *
 * <p>The workspace is vendored at a pinned commit and never modified. See
 * {@code fixtures/petclinic/PROVENANCE.md}.
 */
public final class Candidate {

    /** What the agent was asked, in the words of the proposal it was given. */
    public static final String GOAL = "Add smart appointment scheduling to the PetClinic application.";

    /**
     * The vendored fixtures, located rather than assumed.
     *
     * <p>Maven runs modules with the working directory set to wherever Maven was invoked, and this
     * case study can legitimately be invoked from the repository root or from its own directory.
     * So the fixtures are found by looking in the obvious places rather than by hoping.
     */
    private static final Path FIXTURES = locateFixtures();

    /** The vendored specification directory, root of every rubric this case study reads. */
    public static final Path SPEC = FIXTURES.resolve(
        "appointment-scheduling-spec-with-usecases/spec/smart-appointment-scheduling");

    private Candidate() {
    }

    /**
     * The generated system, materialized on first use.
     *
     * <p>A copy of the pinned fixture with the project's own formatter applied so it compiles.
     * Nothing is fetched and the fixture itself is never touched.
     */
    public static Path workspace() {
        Path workspace = FIXTURES.resolve("build/large-candidate");
        if (!Files.isDirectory(workspace)) {
            System.out.println("Materializing the candidate from the pinned fixture...");
            run(FIXTURES, "./materialize-large-candidate.sh");
        }
        return workspace;
    }

    /** The context a judge is given: the goal, the workspace, and how the run went. */
    public static JudgmentContext contextFor(Path workspace) {
        return JudgmentContext.builder()
            .goal(GOAL)
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(4))
            .build();
    }

    private static Path locateFixtures() {
        List<Path> candidates = List.of(
            Path.of("fixtures/petclinic"),
            Path.of("case-studies/spec-driven-petclinic/fixtures/petclinic"),
            Path.of("../fixtures/petclinic"),
            Path.of("../../fixtures/petclinic"));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("PROVENANCE.md"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not find fixtures/petclinic from "
            + Path.of("").toAbsolutePath() + ". Run from the repository root or from "
            + "case-studies/spec-driven-petclinic.");
    }

    private static void run(Path directory, String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
            if (!process.waitFor(20, TimeUnit.MINUTES) || process.exitValue() != 0) {
                throw new IllegalStateException(String.join(" ", command) + " failed");
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Could not run " + String.join(" ", command), e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted running " + String.join(" ", command), e);
        }
    }
}
