/*
 * Module 07: does the code hold to the architecture that was laid down?
 *
 * Module 06 asked whether the agent built what was asked. This asks something
 * the acceptance criteria never covered: whether the structure the design
 * specified is the structure that exists.
 *
 * Same judge, different document. The 13 rules go in the way the 52 criteria
 * did: verbatim, numbered, all of them answered.
 *
 * Run:               ./mvnw exec:java -pl module-07-architecture-rules
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.rules;

import java.nio.file.Path;
import java.util.List;

import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.architecture.JudgeBackends;
import io.github.markpollack.judge.tutorial.build.PetClinic;
import io.github.markpollack.judge.tutorial.spec.CriteriaAuditJudge;

public class ArchitectureRulesDemo {

    private static final Path RULES = Path.of(
        "fixtures/petclinic/appointment-scheduling-spec-with-usecases",
        "spec/smart-appointment-scheduling/rules.md");

    public static void main(String[] args) {
        System.out.println("=== Module 07: Does the architecture hold? ===\n");
        System.out.println("Backend: " + JudgeBackends.describe() + "\n");

        List<ArchitectureRule> rules = ArchitectureRule.from(RULES);
        Path workspace = PetClinic.largeCandidate();

        System.out.println("Design:  smart-appointment-scheduling, technical design and constraints");
        System.out.println("Rules:   " + rules.size() + ", read verbatim with their own identifiers\n");
        rules.forEach(rule -> System.out.println("  " + pad(rule.id()) + rule.title()));
        System.out.println();

        Judgment judgment = CriteriaAuditJudge
            .create("architecture-rules", "Does the code hold to the architecture the design laid down?",
                workspace, rules, "architecture-rules")
            .judge(PetClinic.contextFor(workspace));

        System.out.println("  architecture-rules  " + judgment.status());
        System.out.println("  " + judgment.reasoning() + "\n");

        judgment.checks().stream().filter(check -> !check.passed()).forEach(check -> {
            System.out.println("  FAIL  " + check.name());
            wrap(check.message());
        });
        String undetermined = String.valueOf(judgment.metadata().getOrDefault("undetermined", ""));
        if (!undetermined.isBlank()) {
            System.out.println("\n  could not be determined: " + undetermined);
        }

        para("""
            Compare the shape of this answer to module 06's. There the criteria
            were mostly settled by reading a line of code, and the judge said so.
            Here they are not. A rule about lock acquisition order is a claim
            about what happens at runtime, and reading the source is a weak way
            to check it.

            That difference is the finding, and it is what module 08 acts on. Some
            of these thirteen are structural facts a compiler-level tool can decide
            outright: which types a field may hold, which packages a class may
            reach, what a controller may return. Those do not need a model, and
            asking one to re-decide them every run is paying for judgment where
            there is nothing to judge.

            The rest genuinely need reading. The question module 08 asks is not
            "can we write an ArchUnit rule" -- you can always write one -- but
            which of these deserve to become policy, and what happens to the ones
            that do not.
            """);

        System.out.println("Done.");
    }

    private static String pad(String id) {
        return (id + "          ").substring(0, 10);
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("        ");
        for (String word : text.split(" ")) {
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
