/*
 * Module 05: The derived oracle
 *
 * A known oracle has one exact answer: the build's exit code, the file's
 * presence. A derived oracle has none - there is no single expected value -
 * but several objective facts together settle the question.
 *
 *     "Is this class where the codebase's layout says it should be?"
 *
 *     the package directory exists
 *     the .java file is in it
 *     the file declares that package
 *
 * No one of those is the answer. All three are the evidence, and the finding
 * is derived from them. Nothing here is interpretive, so nothing here needs a
 * model - this is still deterministic, just not a single assertion.
 *
 * The rule that makes it useful: every fact computed on the way to the
 * verdict is kept as a Check. A verdict can be recomputed from its parts;
 * the parts cannot be recovered from a verdict.
 *
 * Run: ./mvnw exec:java -pl module-05-derived-judge
 */
package io.github.markpollack.judge.tutorial.module05;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.JudgeWithMetadata;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Judgment;

public class DerivedJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 05: The derived oracle ===\n");

        Path workspace = Path.of("test-workspace");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        // PackageStructureJudge extends DeterministicJudge
        PackageStructureJudge judge = new PackageStructureJudge(
            "com.example", "ReportController");

        // Metadata is available from the superclass
        System.out.println("Judge metadata:");
        if (judge instanceof JudgeWithMetadata jwm) {
            System.out.println("  Name:        " + jwm.metadata().name());
            System.out.println("  Description: " + jwm.metadata().description());
            System.out.println("  Type:        " + jwm.metadata().type());
        }

        System.out.println();
        Judgment result = judge.judge(context);

        System.out.println("Result:");
        System.out.println("  Status:    " + result.status());
        System.out.println("  Stored score (optional): " + result.score());
        System.out.println("  Reasoning: " + result.reasoning());

        // The parts. This is the whole reason to extend DeterministicJudge
        // rather than return a bare boolean: the verdict above is derived from
        // these three, and only these three can tell you which one binds.
        System.out.println("\nThe evidence it derived the verdict from:");
        result.checks().forEach(check ->
            System.out.printf("  %s %-15s %s%n",
                check.passed() ? "PASS" : "FAIL",
                check.name(),
                check.message()));

        // Watched red. A judge nobody has seen fail is not yet evidence.
        System.out.println("\n--- A class that is not there ---");
        PackageStructureJudge missingJudge = new PackageStructureJudge(
            "com.example", "MissingController");
        Judgment missingResult = missingJudge.judge(context);

        System.out.println("Result: " + missingResult.status());
        missingResult.checks().forEach(check ->
            System.out.printf("  %s %-15s %s%n",
                check.passed() ? "PASS" : "FAIL",
                check.name(),
                check.message()));

        System.out.println();
        para("""
            Three checks, one finding, and the finding is recomputable from the
            checks at any time. Had this judge stored only PASS or FAIL, the
            question "which part was missing?" would need the agent re-run
            rather than a second look at what it already recorded.

            Module 06 puts this judge into a definition of done alongside the
            build, the coverage measurement, and the semantic verdict.
            """);
        System.out.println("Done.");
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
