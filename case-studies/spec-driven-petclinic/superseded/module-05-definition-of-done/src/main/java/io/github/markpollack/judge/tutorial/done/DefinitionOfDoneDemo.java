/*
 * Module 05: what is our definition of done?
 *
 * Three questions have been answered separately: does it build, did coverage
 * hold, does it follow the conventions. Merging needs all three at once.
 *
 * The rule this module exists to make obvious:
 *
 *     Compose requirements. Aggregate estimates of the same uncertain property.
 *
 * These are three different questions, not three opinions about one, so the
 * composition is a conjunction. There is no vote and no average here. Module 09
 * is the other case, where aggregation is correct.
 *
 * Run: ./mvnw exec:java -pl module-05-definition-of-done
 */
package io.github.markpollack.judge.tutorial.done;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.coverage.CoveragePreservationJudge;
import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.jury.AllMustPassStrategy;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.jury.Verdict;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.agentic.InvestigatingArchitectureJudge;
import io.github.markpollack.judge.tutorial.architecture.JudgeBackends;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class DefinitionOfDoneDemo {

    /** One requirement: what it is called, which kind of oracle answers it, and the judge. */
    record Requirement(String name, String oracle, Judge judge) {
    }

    public static void main(String[] args) {
        System.out.println("=== Module 05: What is our definition of done? ===\n");
        System.out.println("Backend: " + JudgeBackends.describe() + "\n");

        Path workspace = PetClinic.candidateWithCoverage();
        JudgmentContext context = PetClinic.contextBuilder(workspace)
            .metadata("baselineCoverage", PetClinic.BASELINE_LINE_COVERAGE)
            .build();

        List<Requirement> definitionOfDone = List.of(
            new Requirement("build-and-tests", "known",
                BuildSuccessJudge.maven("-o", "test", "jacoco:report")),
            new Requirement("coverage-preserved", "measured",
                new CoveragePreservationJudge(0.0)),
            new Requirement("follows-conventions", "judgment",
                InvestigatingArchitectureJudge.create(workspace, "agentic-architecture-clean")));

        System.out.println("Evaluating " + definitionOfDone.size() + " independent requirements...\n");

        Map<String, Judgment> results = new LinkedHashMap<>();
        for (Requirement requirement : definitionOfDone) {
            results.put(requirement.name(), requirement.judge().judge(context));
        }

        System.out.printf("  %-20s %-10s %s%n", "REQUIREMENT", "ORACLE", "STATUS");
        System.out.printf("  %-20s %-10s %s%n", "-".repeat(20), "-".repeat(10), "------");
        for (Requirement requirement : definitionOfDone) {
            System.out.printf("  %-20s %-10s %s%n",
                requirement.name(), requirement.oracle(), results.get(requirement.name()).status());
        }

        // The same three requirements, through two aggregation rules.
        //
        // Nothing about the judges changes. Only the rule for combining them
        // changes, and the two rules disagree about whether this is mergeable.
        SimpleJury.Builder shared = SimpleJury.builder();
        for (Requirement requirement : definitionOfDone) {
            shared.judge(Judges.named(requirement.judge(), requirement.name()), 1.0);
        }

        Verdict majority = SimpleJury.builder()
            .judge(named(definitionOfDone, 0), 1.0)
            .judge(named(definitionOfDone, 1), 1.0)
            .judge(named(definitionOfDone, 2), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build()
            .vote(context);

        Verdict allMustPass = SimpleJury.builder()
            .judge(named(definitionOfDone, 0), 1.0)
            .judge(named(definitionOfDone, 1), 1.0)
            .judge(named(definitionOfDone, 2), 1.0)
            .votingStrategy(new AllMustPassStrategy())
            .build()
            .vote(context);

        System.out.printf("%n  %-24s %s%n", "MajorityVotingStrategy", majority.aggregated().status());
        System.out.printf("  %-24s %s%n", "AllMustPassStrategy", allMustPass.aggregated().status());

        para("""
            Same three judges. Same evidence. Opposite answers.

            Majority counts votes, so two requirements outvote one and the change
            is mergeable. That is the right rule when several judges estimate the
            same uncertain quantity, and it is the wrong one here, because "the
            tests pass" cannot make up for "it does not follow the conventions it
            was asked to follow".

            AllMustPassStrategy is the rule a definition of done needs: every
            requirement must be met, and a mixed panel is a FAIL rather than a
            shrug. Until recently the library had no name for it and this module
            wrote the conjunction by hand.
            """);

        boolean done = allMustPass.aggregated().status() == JudgmentStatus.PASS;
        System.out.println("  done: " + done);
        para("Would you merge this?");

        long passed = results.values().stream().filter(Judgment::pass).count();
        System.out.printf("%n--- What this must not become ---%n%n  %d of %d passed = %.2f%n",
            passed, results.size(), (double) passed / results.size());
        para("""
            That number is a compensatory rule: it lets strong requirements offset
            weak ones. It is right when several judges estimate the same uncertain
            quantity, and wrong here, because "the tests pass" cannot make up for
            "it does not follow the conventions it was asked to follow".

            A definition of done is conjunctive. The aggregate is a minimum, and it
            is worth less than the row that failed, which is the part you act on.
            """);

        System.out.println("--- The denominator ---\n");
        // Every requirement declined to apply, so nothing was actually checked.
        // Not the same as passing, and not a coding error either: a real panel can
        // abstain its way to an empty population.
        Judgment nothingApplied = new AllMustPassStrategy().aggregate(
            List.of(Judgment.abstain("no baseline recorded"),
                    Judgment.abstain("not applicable to this change")),
            Map.of());
        System.out.println("  AllMustPassStrategy, every requirement abstained = "
            + nothingApplied.status());
        para("""
            ABSTAIN, not PASS. An empty conjunction is vacuously true, so allMatch
            over an empty collection returns true and a definition of done that
            lost its requirements would report done. The strategy refuses to,
            and this module used to carry that guard by hand.

            A pass over an empty input set is not a pass; it is an abstention
            wearing one.

            Next: this is credible for a change of 85 lines that one person can read.
            What happens when an agent writes two hundred files from a specification?
            """);

        System.out.println("Done.");
    }

    private static Judge named(List<Requirement> requirements, int index) {
        Requirement requirement = requirements.get(index);
        return Judges.named(requirement.judge(), requirement.name());
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("      ");
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 76) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("      ");
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
