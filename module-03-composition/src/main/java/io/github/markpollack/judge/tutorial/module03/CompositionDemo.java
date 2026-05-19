/*
 * Module 03: Judge Composition
 *
 * Compose judges with boolean logic: and(), or(), allOf(), anyOf().
 * Judges.and() short-circuits — if the first judge fails, the second never runs.
 *
 * Run: ./mvnw exec:java -pl module-03-composition
 */
package io.github.markpollack.judge.tutorial.module03;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.fs.FileContentJudge;
import io.github.markpollack.judge.fs.FileExistsJudge;
import io.github.markpollack.judge.result.Judgment;

public class CompositionDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 03: Judge Composition Demo ===\n");

        Path workspace = Path.of("test-workspace");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Add a HelloController with hello() method")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        String controllerPath = "src/main/java/com/example/HelloController.java";

        Judge fileExists = new FileExistsJudge(controllerPath);
        Judge hasContent = new FileContentJudge(controllerPath, "hello",
            FileContentJudge.MatchMode.CONTAINS);
        Judge hasMissing = new FileContentJudge(controllerPath, "@RestController",
            FileContentJudge.MatchMode.CONTAINS);

        // AND: both must pass. Short-circuits on first failure.
        System.out.println("--- Judges.and() ---");
        Judge andJudge = Judges.and(fileExists, hasContent);
        Judgment andResult = andJudge.judge(context);
        System.out.println("  file exists AND has 'hello': " + andResult.status());

        // OR: either can pass.
        System.out.println("\n--- Judges.or() ---");
        Judge orJudge = Judges.or(hasContent, hasMissing);
        Judgment orResult = orJudge.judge(context);
        System.out.println("  has 'hello' OR has '@RestController': " + orResult.status());

        // allOf: variadic AND — all must pass.
        System.out.println("\n--- Judges.allOf() ---");
        Judge allJudge = Judges.allOf(fileExists, hasContent, hasMissing);
        Judgment allResult = allJudge.judge(context);
        System.out.println("  all three checks: " + allResult.status());
        System.out.println("  Reasoning: " + allResult.reasoning());

        // anyOf: variadic OR — any can pass.
        System.out.println("\n--- Judges.anyOf() ---");
        Judge anyJudge = Judges.anyOf(hasContent, hasMissing);
        Judgment anyResult = anyJudge.judge(context);
        System.out.println("  any of two content checks: " + anyResult.status());

        System.out.println("\nDone.");
    }
}
