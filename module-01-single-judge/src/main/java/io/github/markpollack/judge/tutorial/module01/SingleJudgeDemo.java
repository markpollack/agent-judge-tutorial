/*
 * Module 01: Single Judge
 *
 * The simplest evaluation: check whether a file exists in the workspace.
 * Demonstrates JudgmentContext creation and single-judge evaluation.
 *
 * Run: ./mvnw exec:java -pl module-01-single-judge
 */
package io.github.markpollack.judge.tutorial.module01;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.fs.FileExistsJudge;
import io.github.markpollack.judge.result.Judgment;

public class SingleJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 01: Single Judge Demo ===\n");

        // The workspace is a directory an agent modified.
        // Judges inspect the workspace to decide pass/fail.
        Path workspace = Path.of("test-workspace");

        // JudgmentContext is the immutable input shared across all judges.
        JudgmentContext context = JudgmentContext.builder()
            .goal("Add a HelloController class")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        System.out.println("Goal:      " + context.goal());
        System.out.println("Workspace: " + context.workspace());
        System.out.println("Status:    " + context.status());
        System.out.println();

        // FileExistsJudge checks whether a file exists in the workspace.
        Judge fileJudge = new FileExistsJudge(
            "src/main/java/com/example/HelloController.java");

        Judgment result = fileJudge.judge(context);

        System.out.println("Judge result:");
        System.out.println("  Status:    " + result.status());
        System.out.println("  Score:     " + result.score());
        System.out.println("  Reasoning: " + result.reasoning());

        // Try a file that doesn't exist
        System.out.println();
        Judge missingJudge = new FileExistsJudge("src/main/java/com/example/MissingFile.java");
        Judgment missingResult = missingJudge.judge(context);

        System.out.println("Missing file judge:");
        System.out.println("  Status:    " + missingResult.status());
        System.out.println("  Reasoning: " + missingResult.reasoning());

        System.out.println("\nDone.");
    }
}
