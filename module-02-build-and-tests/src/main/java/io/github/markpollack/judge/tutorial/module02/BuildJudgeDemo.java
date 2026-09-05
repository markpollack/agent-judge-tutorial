/*
 * Module 02: Build Judge
 *
 * Verify that a Maven project compiles. BuildSuccessJudge runs real
 * build commands in the workspace and checks the exit code.
 *
 * Run: ./mvnw exec:java -pl module-02-build-judge
 */
package io.github.markpollack.judge.tutorial.module02;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.result.Judgment;

public class BuildJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 02: Build Judge Demo ===\n");

        Path workspace = Path.of("test-workspace");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Create a compilable Maven project")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(1))
            .build();

        // BuildSuccessJudge.maven() auto-detects ./mvnw in the workspace.
        // It runs the specified goals and checks the exit code.
        Judge buildJudge = BuildSuccessJudge.maven("compile");

        System.out.println("Running Maven compile in: " + workspace);
        Judgment result = buildJudge.judge(context);

        System.out.println("\nBuild result:");
        System.out.println("  Status:    " + result.status());
        System.out.println("  Stored score (optional): " + result.score());
        System.out.println("  Reasoning: " + result.reasoning());

        // Show checks if present
        if (!result.checks().isEmpty()) {
            System.out.println("  Checks:");
            result.checks().forEach(check ->
                System.out.printf("    %s %s: %s%n",
                    check.passed() ? "PASS" : "FAIL",
                    check.name(),
                    check.message()));
        }

        System.out.println("\nDone.");
    }
}
