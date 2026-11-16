/*
 * Module 02: The build and the tests are already oracles
 *
 * Module 01 needed a model because the criterion had no expected value. Most
 * criteria are not like that. The compiler decides whether the code compiles;
 * JUnit decides whether the tests pass. Both are exact, both are trusted, and
 * neither is part of Agent Eval.
 *
 * What Agent Eval adds is not a better oracle. It is a way for that oracle's
 * result to travel - so a build outcome and a semantic verdict can sit in the
 * same definition of done, which is module 06.
 *
 * Run: ./mvnw exec:java -pl module-02-build-and-tests
 */
package io.github.markpollack.judge.tutorial.module02;

import java.nio.file.Path;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.judgment.Judgment;

public class BuildAndTestsDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    public static void main(String[] args) {
        System.out.println("=== Module 02: The build and the tests are already oracles ===\n");

        // BuildSuccessJudge.maven() prefers ./mvnw in the workspace and falls
        // back to mvn on PATH. It runs the real command and reads the exit code.
        // The exit code is the oracle; the judge is the wrapper around it. The
        // workspace is the evidence, bound when the judge is built.
        System.out.println("--- Does it compile? ---");
        run(BuildSuccessJudge.maven("compile").evidence(WORKSPACE).build());

        System.out.println("\n--- Do the tests pass? ---");
        run(BuildSuccessJudge.maven("test").evidence(WORKSPACE).build());

        // A judge that has never been watched failing is not yet evidence of
        // anything. This goal does not exist, so Maven exits non-zero.
        System.out.println("\n--- And when the oracle says no ---");
        run(BuildSuccessJudge.maven("no-such-goal").evidence(WORKSPACE).build());

        para("""
            Nothing above is an Agent Eval decision. javac decided the first
            one, JUnit decided the second, and Maven's exit code decided the
            third. The judge only carries the answer.

            That is the point. A criterion with a known oracle should keep using
            the instrument that already owns it - and module 06 needs those
            answers in the same shape as the semantic ones so they can sit in one
            definition of done together.
            """);

        System.out.println("Done.");
    }

    private static void run(Judge judge) {
        Judgment result = judge.judge();
        System.out.println("  status     " + result.status());
        System.out.println("  reasoning  " + result.reasoning());
        result.checks().forEach(check ->
            System.out.printf("    %-4s %-20s %s%n",
                check.judgment().status(), check.id(), check.judgment().reasoning()));
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
