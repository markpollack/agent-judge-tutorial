/*
 * Module 06: Lambda Judge
 *
 * The simplest custom judges: lambdas and named lambdas.
 * Judge is a @FunctionalInterface, so any lambda that takes
 * JudgmentContext and returns Judgment is a judge.
 *
 * Run: ./mvnw exec:java -pl module-06-lambda-judge
 */
package io.github.markpollack.judge.tutorial.module06;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.JudgeType;
import io.github.markpollack.judge.JudgeWithMetadata;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.jury.Verdict;
import io.github.markpollack.judge.result.Judgment;

public class LambdaJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 06: Lambda Judge Demo ===\n");

        Path workspace = Path.of("test-workspace");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Create a Maven project with source code")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        // --- Lambda judge (3 lines) ---
        System.out.println("--- Lambda Judge ---");
        Judge pomCheck = ctx -> {
            boolean exists = Files.exists(ctx.workspace().resolve("pom.xml"));
            return exists ? Judgment.pass("pom.xml found") : Judgment.fail("pom.xml missing");
        };

        Judgment pomResult = pomCheck.judge(context);
        System.out.println("  pom.xml check: " + pomResult.status());
        System.out.println("  Reasoning: " + pomResult.reasoning());

        // Lambda judges have no metadata — infrastructure can't discover their name.
        System.out.println("  Has metadata: " + (pomCheck instanceof JudgeWithMetadata));

        // --- Named lambda ---
        System.out.println("\n--- Named Lambda ---");
        Judge namedPom = Judges.named(pomCheck,
            "pom-check",
            "Verifies pom.xml exists",
            JudgeType.DETERMINISTIC);

        // Now infrastructure can discover the name
        if (namedPom instanceof JudgeWithMetadata jwm) {
            System.out.println("  Name: " + jwm.metadata().name());
            System.out.println("  Description: " + jwm.metadata().description());
            System.out.println("  Type: " + jwm.metadata().type());
        }

        // --- Lambda judges in a jury ---
        System.out.println("\n--- Lambda Judges in a Jury ---");
        Judge srcDirCheck = Judges.named(
            ctx -> Files.isDirectory(ctx.workspace().resolve("src"))
                ? Judgment.pass("src/ directory exists")
                : Judgment.fail("src/ directory missing"),
            "src-dir", "Source directory check");

        Judge javaFileCheck = Judges.named(
            ctx -> {
                Path javaDir = ctx.workspace().resolve("src/main/java");
                if (!Files.isDirectory(javaDir)) {
                    return Judgment.fail("No Java source directory");
                }
                try (var stream = Files.walk(javaDir)) {
                    long count = stream.filter(p -> p.toString().endsWith(".java")).count();
                    return count > 0
                        ? Judgment.pass("Found " + count + " Java file(s)")
                        : Judgment.fail("No Java files found");
                } catch (Exception e) {
                    return Judgment.error("Error scanning: " + e.getMessage(), e);
                }
            },
            "java-files", "Java source files present");

        SimpleJury jury = SimpleJury.builder()
            .judge(namedPom, 1.0)
            .judge(srcDirCheck, 1.0)
            .judge(javaFileCheck, 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        Verdict verdict = jury.vote(context);
        System.out.println("  Overall: " + verdict.aggregated().status());
        verdict.individualByName().forEach((name, judgment) ->
            System.out.printf("    %-15s %s  %s%n",
                name, judgment.status(), judgment.reasoning()));

        System.out.println("\nDone.");
    }
}
