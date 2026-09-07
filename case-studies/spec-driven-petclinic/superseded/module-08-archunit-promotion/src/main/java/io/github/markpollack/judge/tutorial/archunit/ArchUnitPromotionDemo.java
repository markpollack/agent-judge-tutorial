/*
 * Module 08: which findings deserve to become policy?
 *
 * Module 07's judge found 8 violations in 13 architectural rules and every
 * one of them checked out. The tempting next step is to write 8 ArchUnit
 * rules. The design document this all came from says not to, and this module
 * is why.
 *
 * Run: ./mvnw exec:java -pl module-08-archunit-promotion
 *
 * No model, no network, no recording. That is the whole point.
 */
package io.github.markpollack.judge.tutorial.archunit;

import java.nio.file.Path;
import java.util.List;

import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class ArchUnitPromotionDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 08: Which findings deserve to become policy? ===\n");
        System.out.println("Backend: none. ArchUnit reads bytecode.\n");

        Path workspace = PetClinic.largeCandidate();

        System.out.println("--- Promoted: decidable from bytecode, and it caught something ---\n");
        long started = System.nanoTime();
        Judgment judgment = ArchUnitJudge.judge(workspace, PromotedRules.all());
        long millis = (System.nanoTime() - started) / 1_000_000;

        System.out.println("  archunit-promoted  " + judgment.status());
        System.out.println("  " + judgment.reasoning() + "  (" + millis + "ms)\n");
        judgment.checks().forEach(check -> System.out.println(
            "  " + (check.passed() ? "PASS  " : "FAIL  ") + check.name() + "  " + check.message()));

        System.out.println();
        PromotedRules.all().forEach(named -> {
            List<String> violations = ArchUnitJudge.violations(workspace, named);
            if (!violations.isEmpty()) {
                System.out.println("  " + named.ruleId() + ", first two of " + violations.size() + ":");
                violations.stream().limit(2).forEach(line -> wrap(shorten(line)));
            }
        });

        para("""
            Module 07 needed a model and a wall-clock wait measured in minutes to
            find these. This found them in %dms with neither, and will keep finding
            them on every build, offline, for as long as the rule lives. That is
            what promotion buys, and it is why it is worth being careful about
            what gets promoted.
            """.formatted(millis));

        System.out.println("--- Not promoted: the rule you would write, and what it does ---\n");
        ShadowRules.all().forEach(named -> {
            List<String> violations = ArchUnitJudge.violations(workspace, named);
            System.out.println("  " + named.ruleId() + " shadow  " + (violations.isEmpty() ? "PASS" : "FAIL")
                + "  " + named.description());
            violations.stream().limit(2).forEach(line -> wrap(shorten(line)));
            System.out.println();
        });

        para("""
            Both of these are honest rules. Neither is a strawman: they mechanise
            the part of the finding that bytecode can see, which is what anyone
            told to mechanise the finding would do.

            RULE-4 shadow is green. The judge found the mandated lock order
            inverted in two methods. Order is a runtime sequence and dependency
            analysis has no notion of it, so the half that can be checked is the
            half that was not broken.

            RULE-8 shadow is worse than green. It reports two private helpers
            that return an Owner internally -- which RULE-8 permits -- and misses
            all five entities going into the views in those same two files. Act on
            it and you would change the wrong methods and believe you were done.

            A judgment replaced by a check that cannot express it has not been
            promoted. It has been lost, and the reassuring result is what makes
            the loss hard to see.
            """);

        System.out.println("--- Deterministic, and not ArchUnit's business ---\n");
        List<Check> config = List.of(
            ConfigRules.flywayOwnsTheSchema(workspace),
            ConfigRules.mavenOwnsTheDatabaseMatrix(workspace));
        config.forEach(check -> {
            System.out.println("  " + (check.passed() ? "PASS  " : "FAIL  ") + check.name());
            wrap(check.message());
        });

        para("""
            Two more rules are as mechanisable as anything above and have nothing
            to do with bytecode. One is a fact about properties, one about a YAML
            file, and both are exact. Reaching for ArchUnit because ArchUnit is the
            architecture tool would have found nothing.

            So the thirteen came apart four ways: two became ArchUnit rules, two
            became file assertions, and nine stayed with the judge -- including
            the two most serious findings in the whole audit. That is not a
            failure of mechanisation. It is the answer to the question, and the
            reason the judge does not get retired once the rules exist.
            """);

        System.out.println("Done.");
    }

    private static String shorten(String line) {
        return line.replace("org.springframework.samples.petclinic.", "")
            .replaceAll("[<>]", "").replaceAll("^(Method|Constructor) ", "").strip();
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("        ");
        for (String word : text.strip().split("\\s+")) {
            if (line.length() + word.length() > 76 && line.length() > 8) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("        ");
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
