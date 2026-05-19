/*
 * Module 07: Deterministic Judge
 *
 * Build a reusable judge by extending DeterministicJudge.
 * Gets metadata (name, description, type) for logging and verdict reporting,
 * plus granular Check sub-assertions that pinpoint exactly what failed.
 *
 * Run: ./mvnw exec:java -pl module-07-deterministic-judge
 */
package io.github.markpollack.judge.tutorial.module07;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.JudgeWithMetadata;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Judgment;

public class DeterministicJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 07: Deterministic Judge Demo ===\n");

        Path workspace = Path.of("test-workspace");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Add HelloController in com.example package")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        // PackageStructureJudge extends DeterministicJudge
        PackageStructureJudge judge = new PackageStructureJudge(
            "com.example", "HelloController");

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
        System.out.println("  Score:     " + result.score());
        System.out.println("  Reasoning: " + result.reasoning());

        // Granular checks pinpoint exactly what passed or failed
        System.out.println("\nChecks:");
        result.checks().forEach(check ->
            System.out.printf("  %s %-15s %s%n",
                check.passed() ? "PASS" : "FAIL",
                check.name(),
                check.message()));

        // Try with a class that doesn't exist
        System.out.println("\n--- Missing class ---");
        PackageStructureJudge missingJudge = new PackageStructureJudge(
            "com.example", "MissingController");
        Judgment missingResult = missingJudge.judge(context);

        System.out.println("Result: " + missingResult.status());
        missingResult.checks().forEach(check ->
            System.out.printf("  %s %-15s %s%n",
                check.passed() ? "PASS" : "FAIL",
                check.name(),
                check.message()));

        System.out.println("\nDone.");
    }
}
