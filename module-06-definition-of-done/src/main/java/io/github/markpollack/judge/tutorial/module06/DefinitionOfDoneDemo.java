/*
 * Module 06: The definition of done
 *
 * Five criteria, four instruments, three kinds of oracle, one question:
 * is this done?
 *
 * The rule this module exists to make obvious:
 *
 *     Compose requirements. Aggregate estimates of the same uncertain property.
 *
 * These five criteria are not five opinions about one thing. They are five
 * different things, and every one of them has to hold. Module 08 is the other
 * case, where aggregation is the right answer.
 *
 * Run: ./mvnw exec:java -pl module-06-definition-of-done
 */
package io.github.markpollack.judge.tutorial.module06;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.coverage.CoveragePreservationJudge;
import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.module01.ArchitecturalFitJudge;
import io.github.markpollack.judge.tutorial.module05.PackageStructureJudge;

public class DefinitionOfDoneDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    private static final String SUBJECT = "src/main/java/com/example/ReportController.java";

    private static final String GOAL = "Expose a sales report endpoint";

    /** One requirement: what it is called, which kind of oracle answers it, and the judge. */
    record Criterion(String name, String oracle, Judge judge) {
    }

    public static void main(String[] args) {
        System.out.println("=== Module 06: The definition of done ===\n");

        // The architectural-fit judge needs the sources in metadata; every other
        // judge reads the workspace. One context serves all five.
        JudgmentContext context = ArchitecturalFitJudge
            .contextBuilder(WORKSPACE, GOAL, SUBJECT)
            .metadata("baselineCoverage", 100.0)
            .build();

        List<Criterion> definitionOfDone = List.of(
            new Criterion("build", "known",
                BuildSuccessJudge.maven("compile")),
            new Criterion("tests", "known",
                BuildSuccessJudge.maven("test")),
            new Criterion("coverage", "known",
                new CoveragePreservationJudge(5.0)),
            new Criterion("package-structure", "derived",
                new PackageStructureJudge("com.example", "ReportController")),
            new Criterion("architectural-fit", "judgment",
                ArchitecturalFitJudge.create()));

        // ---------------------------------------------------------------
        // Every criterion evaluated. Every result kept.
        // ---------------------------------------------------------------
        System.out.println("Evaluating " + definitionOfDone.size() + " criteria...\n");

        List<Judgment> results = definitionOfDone.stream()
            .map(criterion -> criterion.judge().judge(context))
            .toList();

        System.out.printf("  %-18s %-10s %s%n", "CRITERION", "ORACLE", "STATUS");
        System.out.printf("  %-18s %-10s %s%n", "-".repeat(18), "-".repeat(10), "------");
        for (int i = 0; i < definitionOfDone.size(); i++) {
            Criterion criterion = definitionOfDone.get(i);
            System.out.printf("  %-18s %-10s %s%n",
                criterion.name(), criterion.oracle(), results.get(i).status());
        }

        // ---------------------------------------------------------------
        // The composition rule is a conjunction, and it needs a denominator.
        // ---------------------------------------------------------------
        boolean done = isDone(results);
        System.out.println("\n  done: " + done);
        para("Would you merge this?");

        System.out.println("--- What the failures were ---\n");
        for (int i = 0; i < definitionOfDone.size(); i++) {
            Judgment result = results.get(i);
            if (result.status() != JudgmentStatus.PASS) {
                System.out.println("  " + definitionOfDone.get(i).name() + ":");
                wrap(result.reasoning());
            }
        }

        // ---------------------------------------------------------------
        // What this must not become.
        // ---------------------------------------------------------------
        long passed = results.stream().filter(Judgment::pass).count();
        System.out.printf("%n--- What this must not become ---%n%n  %d of %d passed = %.2f%n",
            passed, results.size(), (double) passed / results.size());
        para("""
            That number is a compensatory rule: it lets strong criteria offset
            weak ones. It is the right rule when several judges are estimating
            the same uncertain quantity, and the wrong one here, because
            "the tests pass" cannot make up for "it does not compile".

            A definition of done is conjunctive. Every criterion binds, so the
            aggregate is a minimum, not a mean — and the aggregate is worth
            less than the row that failed, which is the part you act on.
            """);

        // ---------------------------------------------------------------
        // Judges.allOf gives you the conjunction, but not the roster.
        // ---------------------------------------------------------------
        System.out.println("--- Judges.allOf(), and what it costs ---\n");
        // Count invocations so short-circuiting is something you can see rather
        // than something the comment claims.
        AtomicInteger invoked = new AtomicInteger();
        Judge all = Judges.allOf(definitionOfDone.stream()
            .map(criterion -> (Judge) context1 -> {
                invoked.incrementAndGet();
                return criterion.judge().judge(context1);
            })
            .toArray(Judge[]::new));

        Judgment composed = all.judge(context);
        System.out.println("  status:    " + composed.status());
        System.out.println("  reasoning:");
        wrap(composed.reasoning());
        System.out.printf("  judges invoked: %d of %d%n", invoked.get(), definitionOfDone.size());
        para("""
            The status is right and the diagnosis is gone. allOf() short-circuits,
            so evaluation stopped at the first failure and the criteria after it
            never ran — including architectural-fit, which would also have failed.
            You learn that something failed, not what, and not how much.

            Use it for a cheap gate. Do not use it as a definition of done.
            """);

        // ---------------------------------------------------------------
        // The denominator.
        // ---------------------------------------------------------------
        System.out.println("--- A definition of done with nothing in it ---\n");
        System.out.println("  isDone(List.of()) = " + isDone(List.of()));
        para("""
            allMatch() over an empty list returns true, so an empty definition of
            done reports done. A pass over an empty input set is not a pass; it is
            an abstention wearing a pass. State the size of the set you examined,
            always, and require it to be non-empty.
            """);

        System.out.println("Done.");
    }

    /**
     * The composition rule.
     *
     * <p>Conjunctive: every criterion must hold. The count guard is not
     * defensive tidiness — {@code allMatch} over an empty stream is {@code true},
     * so without it a definition of done that lost its criteria would report
     * success.
     */
    private static boolean isDone(List<Judgment> results) {
        return !results.isEmpty()
            && results.stream().allMatch(result -> result.status() == JudgmentStatus.PASS);
    }

    /** Wrap long reasoning so it stays readable in an 80-column terminal. */
    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("    ");
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 76) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("    ");
            }
            line.append(word).append(' ');
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
