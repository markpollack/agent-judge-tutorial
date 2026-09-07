/*
 * Module 02: What does the measured evidence say?
 *
 * Module 01 asked a yes/no question and the exit code answered it. This one
 * cannot be answered that way.
 *
 * A coverage judge does more work than it looks like. It has to find a report
 * that something else had to be asked to produce, parse it, turn it into a
 * measurement, compare that against a baseline somebody recorded, and only then
 * apply a policy. Five steps, and only the last one is an opinion.
 *
 *     instrumentation -> test run -> report -> parser -> measurement
 *                                                            |
 *                                                       baseline
 *                                                            |
 *                                                         policy -> Judgment
 *
 * Run: ./mvnw exec:java -pl module-02-coverage-evidence
 */
package io.github.markpollack.judge.tutorial.coverage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.coverage.CoveragePreservationJudge;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class CoverageEvidenceDemo {

    /**
     * The largest coverage drop this change may cause, in percentage points.
     *
     * Zero, and the derivation matters more than the number. The agent was asked to add a
     * feature. New production code that its author did not test is exactly what a coverage
     * drop detects here, so any drop at all is the signal we care about.
     *
     * The library's default is 5.0, which is a sensible tolerance for a refactor across a
     * large codebase and the wrong bar for one small feature. A default is not a derivation.
     */
    private static final double MAX_ACCEPTABLE_DROP = 0.0;

    public static void main(String[] args) throws IOException {
        System.out.println("=== Module 02: What does the measured evidence say? ===\n");

        Path workspace = PetClinic.candidateWithCoverage();

        // The baseline is a recorded measurement of the code the agent started from,
        // not a target somebody hoped for. It travels in the context, because the
        // judge cannot know what "before" was.
        JudgmentContext context = PetClinic.contextBuilder(workspace)
            .metadata("baselineCoverage", PetClinic.BASELINE_LINE_COVERAGE)
            .build();

        System.out.println("Evidence:  " + workspace);
        System.out.println("             /" + PetClinic.COVERAGE_REPORT);
        System.out.println("Baseline:  " + PetClinic.BASELINE_LINE_COVERAGE + "%  (measured on the pinned baseline)");
        System.out.println("Policy:    a drop of at most " + MAX_ACCEPTABLE_DROP + " pp\n");

        Judgment judgment = new CoveragePreservationJudge(MAX_ACCEPTABLE_DROP).judge(context);
        report(judgment);

        para("""
            The agent added 15 lines of production code and tests that reach them,
            so coverage went up rather than down. That is the measurement. Whether
            it is acceptable is a separate decision, and it is ours.

            Read what the judgment kept: baseline, current, drop, and threshold are
            all in its metadata. A judge that stored only PASS would have thrown
            away every number a reviewer would ask for next.
            """);

        // ------------------------------------------------------------------
        // The same judge, on a run where the agent shipped no tests.
        // ------------------------------------------------------------------
        System.out.println("--- The same judge, on a change with no tests ---\n");
        Path seeded = recordedSeededRegression();
        Judgment regressed = new CoveragePreservationJudge(MAX_ACCEPTABLE_DROP)
            .judge(PetClinic.contextBuilder(seeded)
                .metadata("baselineCoverage", PetClinic.BASELINE_LINE_COVERAGE)
                .build());
        report(regressed);

        para("""
            FAIL, and the number says how badly. This is a real JaCoCo report from
            a real build of the same change with its test class removed, recorded
            so the failure can be shown without paying for another build. See
            fixtures/petclinic/evidence/README.md for the command that produced it.

            A judge nobody has watched fail is not evidence of anything.
            """);

        // ------------------------------------------------------------------
        // And when the evidence was never produced at all.
        // ------------------------------------------------------------------
        System.out.println("--- The same judge, with no evidence at all ---\n");
        Path empty = Files.createTempDirectory("no-build-ran");
        report(new CoveragePreservationJudge(MAX_ACCEPTABLE_DROP)
            .judge(PetClinic.contextBuilder(empty)
                .metadata("baselineCoverage", PetClinic.BASELINE_LINE_COVERAGE)
                .build()));
        Files.deleteIfExists(empty);

        para("""
            ERROR, not FAIL and not zero. Nothing about this workspace was
            measured, so nothing about it was rejected. Scoring it zero would
            blame the change for a report that was never written.

            Next: the build passed and coverage held. Neither of them can tell you
            whether the agent followed the conventions it was asked to follow.
            """);

        System.out.println("Done.");
    }

    /**
     * A workspace whose only content is a recorded JaCoCo report.
     *
     * <p>{@code CoveragePreservationJudge} reads {@code target/site/jacoco/jacoco.xml} under the
     * workspace, so the recorded report is placed where a real build would have left it.
     */
    private static Path recordedSeededRegression() throws IOException {
        Path recorded = Path.of("fixtures/petclinic/evidence/seeded-no-owner-tests.jacoco.xml");
        Path workspace = Files.createTempDirectory("seeded-no-owner-tests");
        Path report = workspace.resolve(PetClinic.COVERAGE_REPORT);
        Files.createDirectories(report.getParent());
        Files.copy(recorded, report);
        workspace.toFile().deleteOnExit();
        return workspace;
    }

    private static void report(Judgment judgment) {
        System.out.println("  status     " + judgment.status());
        wrap(judgment.reasoning());
        judgment.checks().forEach(check ->
            System.out.printf("    %-4s %-26s %s%n",
                check.passed() ? "PASS" : "FAIL", check.name(), check.message()));
        for (String key : new String[] { "baselineLineCoverage", "currentLineCoverage", "coverageDrop", "threshold" }) {
            Object value = judgment.metadata().get(key);
            if (value instanceof Number number) {
                System.out.printf("    %-26s %8.2f%n", key, number.doubleValue());
            }
        }
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("  ");
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 76) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("  ");
            }
            line.append(word).append(' ');
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
