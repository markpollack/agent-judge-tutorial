/*
 * Module 02: The build and the tests are already oracles
 *
 * Module 01 needed a model because the criterion had no expected value. Most
 * criteria are not like that. The compiler decides whether the code compiles;
 * JUnit decides whether the tests pass. Both are exact, both are trusted, and
 * neither is part of Agent Judge.
 *
 * What Agent Judge adds is not a better oracle. It is a way for that oracle's
 * result to travel - so a build outcome and a semantic verdict can sit in the
 * same definition of done, which is module 06.
 *
 * Run: ./mvnw exec:java -pl module-02-build-and-tests
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

public class BuildAndTestsDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    public static void main(String[] args) {
        System.out.println("=== Module 02: The build and the tests are already oracles ===\n");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(WORKSPACE)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .build();

        // BuildSuccessJudge.maven() prefers ./mvnw in the workspace and falls
        // back to mvn on PATH. It runs the real command and reads the exit code.
        // The exit code is the oracle; the judge is the wrapper around it.
        System.out.println("--- Does it compile? ---");
        run(BuildSuccessJudge.maven("compile"), context);

        System.out.println("\n--- Do the tests pass? ---");
        run(BuildSuccessJudge.maven("test"), context);

        // A judge that has never been watched failing is not yet evidence of
        // anything. This goal does not exist, so Maven exits non-zero.
        System.out.println("\n--- And when the oracle says no ---");
        run(BuildSuccessJudge.maven("no-such-goal"), context);

        para("""
            Nothing above is an Agent Judge decision. javac decided the first
            one, JUnit decided the second, and Maven's exit code decided the
            third. The judge only carries the answer.

            That is the point. A criterion with a known oracle should keep using
            the instrument that already owns it - and module 06 needs those
            answers in the same shape as the semantic ones so they can sit in one
            definition of done together.
            """);

        System.out.println("Done.");
    }

    private static void run(Judge judge, JudgmentContext context) {
        Judgment result = judge.judge(context);
        System.out.println("  status     " + result.status());
        System.out.println("  reasoning  " + result.reasoning());
        result.checks().forEach(check ->
            System.out.printf("    %-4s %-20s %s%n",
                check.passed() ? "PASS" : "FAIL", check.name(), check.message()));
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
