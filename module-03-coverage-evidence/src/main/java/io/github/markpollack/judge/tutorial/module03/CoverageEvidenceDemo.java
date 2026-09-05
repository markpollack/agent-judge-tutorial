/*
 * Module 03: Coverage as measured evidence
 *
 * Modules 01 and 02 asked yes/no questions. Coverage is not one: it is a
 * measurement, and a measurement is not an acceptance decision until somebody
 * writes down a bar.
 *
 * This module keeps the three apart on purpose:
 *
 *     the measurement   100.00% -> 36.00%, a 64.0 pp drop
 *     the score         0.0, which is a floor and not a distance
 *     the status        FAIL, because 64.0 pp is past the bar we chose
 *
 * Run: ./mvnw exec:java -pl module-03-coverage-evidence
 */
package io.github.markpollack.judge.tutorial.module03;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.coverage.CoverageImprovementJudge;
import io.github.markpollack.judge.coverage.CoveragePreservationJudge;
import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.result.Judgment;

public class CoverageEvidenceDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    private static final Path REPORT = WORKSPACE.resolve("target/site/jacoco/jacoco.xml");

    /**
     * Measured on the workspace as it stood before the agent added
     * ReportController: 9 of 9 lines covered. See test-workspace/README.md for
     * how to reproduce it. This is a recorded measurement, not a target.
     */
    private static final double BASELINE_LINE_COVERAGE = 100.0;

    /**
     * The largest coverage drop we will accept without a conversation, in
     * percentage points.
     *
     * 5.0 is the library's default, and a default is not a derivation. A real
     * bar comes from somewhere: the drop your team has historically waved
     * through, or the point at which review actually starts. Ours is inherited
     * and provisional, and it is written here rather than three call sites away
     * because this is where the next person changing it will be standing.
     */
    private static final double MAX_ACCEPTABLE_DROP = 5.0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Module 03: Coverage as measured evidence ===\n");

        // ---------------------------------------------------------------
        // A measurement needs evidence, and the evidence has to be produced.
        // ---------------------------------------------------------------
        if (!Files.exists(REPORT)) {
            System.out.println("No JaCoCo report yet. Producing the evidence first...");
            Judgment build = BuildSuccessJudge.maven("test").judge(context());
            System.out.println("  build+tests: " + build.status() + " - " + build.reasoning() + "\n");
        }

        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(WORKSPACE)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .metadata("baselineCoverage", BASELINE_LINE_COVERAGE)
            .build();

        // ---------------------------------------------------------------
        // 1. The measurement.
        // ---------------------------------------------------------------
        System.out.println("--- The measurement ---");
        Judgment measured = new CoverageImprovementJudge().judge(context);

        System.out.printf("  baseline   %.2f%%%n", asDouble(measured, "baselineLineCoverage"));
        System.out.printf("  current    %.2f%%%n", asDouble(measured, "currentLineCoverage"));
        System.out.printf("  delta      %+.1f pp%n", asDouble(measured, "improvementPp"));

        // ---------------------------------------------------------------
        // 2. The score - and what it cannot tell you.
        // ---------------------------------------------------------------
        System.out.println("\n--- The score ---");
        System.out.println("  score      " + measured.score());
        para("""
            The score is normalized to [0.0, 1.0] and floors at 0.0, so it reads
            the same for a 1-point drop and a 64-point one. The distance survived
            only because the judge also recorded the parts. Read the parts.
            """);
        System.out.println("  the parts it kept:");
        measured.checks().forEach(check ->
            System.out.printf("    %-4s %-20s %s%n",
                check.passed() ? "PASS" : "FAIL", check.name(), check.message()));

        // ---------------------------------------------------------------
        // 3. The status - a separate decision, against a bar somebody chose.
        // ---------------------------------------------------------------
        System.out.println("\n--- The acceptance decision ---");
        Judge gate = new CoveragePreservationJudge(MAX_ACCEPTABLE_DROP);
        Judgment accepted = gate.judge(context);

        System.out.printf("  bar        %.1f pp drop, at most%n", MAX_ACCEPTABLE_DROP);
        System.out.println("  status     " + accepted.status());
        System.out.println("  reasoning");
        wrap(accepted.reasoning());
        accepted.checks().forEach(check ->
            System.out.printf("    %-4s %-24s %s%n",
                check.passed() ? "PASS" : "FAIL", check.name(), check.message()));

        // ---------------------------------------------------------------
        // 4. A measurement that could not be taken is not a measurement of 0.
        // ---------------------------------------------------------------
        System.out.println("\n--- No evidence at all ---");
        Path empty = Files.createTempDirectory("no-build-ran");
        Judgment missing = gate.judge(JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(empty)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(1))
            .metadata("baselineCoverage", BASELINE_LINE_COVERAGE)
            .build());

        System.out.println("  status     " + missing.status());
        System.out.println("  reasoning");
        wrap(missing.reasoning());
        para("""
            ERROR, not FAIL and not 0.0. The workspace was never judged; the judge
            could not run. Scoring it zero would blame the subject for a missing
            report, and averaging that zero into anything would be worse.
            Module 09 is where a jury has to decide what to do about it.
            """);

        Files.deleteIfExists(empty);
        System.out.println("Done.");
    }

    private static double asDouble(Judgment judgment, String key) {
        Object value = judgment.metadata().get(key);
        if (!(value instanceof Number number)) {
            // NaN would print, and printing is not the same as knowing. If the
            // evidence is not there, say so rather than rendering a shrug.
            throw new IllegalStateException("Judgment carried no numeric '" + key + "': " + judgment.metadata().keySet());
        }
        return number.doubleValue();
    }

    /** Wrap long reasoning so it stays readable in an 80-column terminal. */
    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("    ");
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 76) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("    ");
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

    private static JudgmentContext context() {
        return JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(WORKSPACE)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .build();
    }
}
