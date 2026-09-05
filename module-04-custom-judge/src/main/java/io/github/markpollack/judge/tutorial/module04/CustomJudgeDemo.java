/*
 * Module 04: Write a judge
 *
 * Modules 01 to 03 used judges. This one writes them, and the whole job is
 * four steps:
 *
 *     task -> criterion -> evidence -> judgment
 *
 * Judge is a @FunctionalInterface, so the smallest judge that can exist is a
 * lambda taking a JudgmentContext and returning a Judgment. Start there and
 * add only what the criterion actually needs.
 *
 * Run: ./mvnw exec:java -pl module-04-custom-judge
 */
package io.github.markpollack.judge.tutorial.module04;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.JudgeType;
import io.github.markpollack.judge.JudgeWithMetadata;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

public class CustomJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 04: Write a judge ===\n");

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

        // --- Three lambdas, all of them required ---
        //
        // Note what this is not: a vote. These are three different criteria,
        // not three estimates of one, so the composition is a conjunction and
        // every result stays visible. Module 06 builds this out properly;
        // module 08 is the case where voting is right.
        System.out.println("\n--- Three criteria, all required ---");

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
                    // State the denominator. "No Java files found" and "found 4"
                    // are different facts, and a bare PASS hides which one it was.
                    return count > 0
                        ? Judgment.pass("Found " + count + " Java file(s)")
                        : Judgment.fail("No Java files found");
                }
                catch (Exception e) {
                    // The judge could not complete. That is ERROR, not FAIL:
                    // the workspace has not been rejected, it has not been read.
                    return Judgment.error("Error scanning: " + e.getMessage());
                }
            },
            "java-files", "Java source files present");

        List<Judge> required = List.of(namedPom, srcDirCheck, javaFileCheck);
        List<Judgment> results = required.stream().map(judge -> judge.judge(context)).toList();

        for (int i = 0; i < required.size(); i++) {
            System.out.printf("    %-12s %-6s %s%n",
                Judges.tryMetadata(required.get(i)).map(m -> m.name()).orElse("?"),
                results.get(i).status(), results.get(i).reasoning());
        }

        boolean allHold = !results.isEmpty()
            && results.stream().allMatch(r -> r.status() == JudgmentStatus.PASS);
        System.out.println("\n    all three hold: " + allHold);

        System.out.println("\nDone.");
    }
}
