/*
 * Module 01: Where JUnit stops
 *
 * The agent says it is done. Should I merge?
 *
 * Some of that question has a known oracle — an exact answer exists and we can
 * write it down in advance. That is what assertTrue is, and Agent Judge is not
 * an improvement on it.
 *
 * The rest of the question does not. This module is the boundary between them.
 *
 * Run: ./mvnw exec:java -pl module-01-oracle-boundary
 */
package io.github.markpollack.judge.tutorial.module01;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.fs.FileExistsJudge;
import io.github.markpollack.judge.result.Judgment;

public class OracleBoundaryDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    private static final String SUBJECT = "src/main/java/com/example/ReportController.java";

    private static final String GOAL = "Expose a sales report endpoint";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Module 01: Where JUnit stops ===\n");
        System.out.println("The agent says it is done. It added ReportController.java for");
        System.out.println("\"" + GOAL + "\".\n");
        System.out.println("Should we merge?\n");

        Path subject = WORKSPACE.resolve(SUBJECT);
        String source = Files.readString(subject);

        // ---------------------------------------------------------------
        // Criteria whose oracle we already know.
        //
        // These are the assertions in OracleBoundaryTest, evaluated here so
        // you can see them next to the one that follows. We knew every answer
        // when we wrote them down — which is exactly what makes them
        // assertions rather than judgments.
        // ---------------------------------------------------------------
        System.out.println("--- Known oracle: ordinary Java testing already answers this ---\n");

        report("the file exists", Files.exists(subject));
        report("it declares package com.example", source.contains("package com.example;"));
        report("the class is named ReportController", source.contains("class ReportController"));
        report("it has the requested report(...) method", source.contains("String report("));

        para("""
            Four for four, and none of them needed a judge. Use the least
            interpretive instrument that can reliably answer the question:
            here that is assertTrue, and a judge would be a step backwards.
            """);

        // A deterministic judge is the same known oracle wearing a different
        // coat. Worth having when a jury needs the result as evidence; not
        // worth reaching for when you are already inside a JUnit test.
        JudgmentContext context =
            ArchitecturalFitJudge.contextFor(WORKSPACE, GOAL, SUBJECT);
        Judge fileExists = new FileExistsJudge(SUBJECT);
        Judgment exists = fileExists.judge(context);
        System.out.println("  FileExistsJudge, for comparison: " + exists.status()
            + "  (same oracle, other instrument)");

        // ---------------------------------------------------------------
        // The criterion nobody wrote an assertEquals for.
        // ---------------------------------------------------------------
        System.out.println("\n--- The criterion nobody wrote an assertEquals for ---");
        para("""
            "Does ReportController fit the architectural conventions and idioms
             of this codebase, without introducing unnecessary complexity?"

            There is no expected value to compare against. The oracle is not
            known in advance; it has to be produced. That is what a judge is.
            """);

        Judgment fit = ArchitecturalFitJudge.create().judge(context);

        System.out.println("  architectural-fit  " + fit.status());
        wrap(fit.reasoning());
        System.out.println();
        fit.checks().forEach(check ->
            System.out.printf("    %-4s  %-18s %s%n",
                check.passed() ? "PASS" : "FAIL", check.name(), check.message()));

        para("""
            The judgment keeps its parts. Five failing criteria and one passing
            one are not averaged into 0.17, because a number cannot tell you
            which criterion binds — and the binding criterion is the only part
            anybody acts on.

            One of those lines is mechanisable: concatenating request parameters
            into SQL is a pattern a scanner should own, and module 05 builds
            exactly that. The verdict is the part that is left over.
            """);

        // ---------------------------------------------------------------
        // The same judge, on the controller the codebase already had.
        // ---------------------------------------------------------------
        System.out.println("\n--- The same judge, on the idiomatic controller ---\n");
        Judgment control = ArchitecturalFitJudge.create().judge(ArchitecturalFitJudge.contextFor(
            WORKSPACE, GOAL, "src/main/java/com/example/HelloController.java"));
        System.out.println("  architectural-fit  " + control.status());
        wrap(control.reasoning());
        para("""
            A judge that can only say FAIL is not a judge, it is a constant.
            This one discriminates, and OracleBoundaryTest asserts both
            directions.
            """);

        System.out.println("\n--- Where this goes ---");
        para("""
            JUnit handles assertions where we know how to write the oracle.
            Agent Judge extends that same testing discipline to criteria whose
            oracle needs richer evidence, or judgment.

            Modules 02 and 03 widen the known oracle: the build, the tests, and
            a real coverage measurement. Module 06 puts every criterion into one
            explicit definition of done, with the failures still visible.
            """);
        System.out.println("Done.");
    }

    /** Indented prose. Text blocks strip their own indentation, so put it back once, here. */
    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }

    /** Wrap long reasoning so it stays readable in an 80-column terminal. */
    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("  ");
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 76) {
                System.out.println(line);
                line = new StringBuilder("  ");
            }
            line.append(word).append(' ');
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static void report(String criterion, boolean satisfied) {
        System.out.printf("  %-4s  %s%n", satisfied ? "PASS" : "FAIL", criterion);
    }
}
