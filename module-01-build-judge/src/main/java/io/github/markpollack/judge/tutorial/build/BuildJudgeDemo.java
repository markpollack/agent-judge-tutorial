/*
 * Module 01: Can it build?
 *
 * An agent changed a real Spring project and says it is done. Should you merge?
 *
 * The first part of that question is not a matter of opinion, and you already
 * own the instrument that answers it. javac decides whether it compiles. The
 * project's formatter decides whether it is formatted. JUnit decides whether the
 * tests pass. Maven's exit code carries all three.
 *
 * Agent Judge does not improve on any of that. What it adds is a shape: an
 * engineering fact becomes a Judgment that other criteria can sit beside, which
 * is what module 05 needs.
 *
 * Run: ./mvnw exec:java -pl module-01-build-judge
 */
package io.github.markpollack.judge.tutorial.build;

import java.nio.file.Path;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.result.Judgment;

public class BuildJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 01: Can it build? ===\n");
        System.out.println("The agent was asked:");
        System.out.println("  \"Add a way to find owners by their city.");
        System.out.println("   Follow the conventions already used in the codebase.\"");
        System.out.println("\nIt says it is done.\n");

        Path workspace = PetClinic.candidateWorkspace();
        JudgmentContext context = PetClinic.contextFor(workspace);

        // BuildSuccessJudge.maven() prefers ./mvnw in the workspace and falls back
        // to mvn on PATH. It runs the real command and reads the exit code.
        //
        // -o keeps the run offline, so the answer cannot depend on the network.
        Judge buildsAndTests = BuildSuccessJudge.maven("-o", "test", "jacoco:report");

        System.out.println("Workspace: " + workspace);
        System.out.println("Command:   ./mvnw -o test jacoco:report");
        System.out.println("\nRunning the real build. This takes about 18 seconds.\n");

        long started = System.currentTimeMillis();
        Judgment judgment = buildsAndTests.judge(context);
        long elapsed = (System.currentTimeMillis() - started) / 1000;

        report(judgment, elapsed + "s");

        para("""
            That is a Judgment. Status first, reasoning next, and the checks the
            judge recorded on the way. Nothing here was decided by Agent Judge:
            javac, spring-javaformat and JUnit decided, and the exit code carried
            their answer.

            What one PASS actually establishes here is worth saying out loud:
            the change compiles, it satisfies the formatter the project enforces
            at build time, and all 74 tests pass, including the 3 the agent added
            and the 71 that were already there.

            The command also asks JaCoCo to write its report. That costs nothing
            here and leaves the evidence module 02 measures.
            """);

        // A judge nobody has watched fail is not yet evidence of anything. This
        // goal does not exist, so Maven exits non-zero. It costs two seconds and
        // proves the exit code is really the oracle.
        System.out.println("--- The same judge, when the oracle says no ---\n");
        report(BuildSuccessJudge.maven("-o", "no-such-goal").judge(context), null);

        para("""
            FAIL, not ERROR. The judge ran, the build ran, and the build said no.
            Module 10 is where the difference between those two matters.

            Next: the build passing does not tell you whether the agent tested
            what it wrote. Module 02 measures that.
            """);

        System.out.println("Done.");
    }

    private static void report(Judgment judgment, String elapsed) {
        System.out.println("  status     " + judgment.status() + (elapsed == null ? "" : "   (" + elapsed + ")"));
        System.out.println("  reasoning  " + judgment.reasoning());
        judgment.checks().forEach(check ->
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
