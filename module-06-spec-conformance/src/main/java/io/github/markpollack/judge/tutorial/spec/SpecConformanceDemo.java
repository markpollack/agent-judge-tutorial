/*
 * Module 06: did it build what was asked?
 *
 * An agent was given a specification and produced 166 Java files. The
 * specification listed, for one use case alone, 52 numbered acceptance
 * criteria. This module asks all 52.
 *
 * Not five of them. A specification with 52 criteria has 52, and a judge that
 * answers a sample has not evaluated the specification. It has sampled it, and
 * then somebody asks about the next five.
 *
 * Run:               ./mvnw exec:java -pl module-06-spec-conformance
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.spec;

import java.nio.file.Path;
import java.util.List;

import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.architecture.JudgeBackends;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class SpecConformanceDemo {

    private static final Path SPEC = Path.of(
        "fixtures/petclinic/appointment-scheduling-spec-with-usecases",
        "spec/smart-appointment-scheduling/manage-appointment-lifecycle/criteria.md");

    public static void main(String[] args) {
        System.out.println("=== Module 06: Did it build what was asked? ===\n");
        System.out.println("Backend: " + JudgeBackends.describe() + "\n");

        List<SpecCriteria> criteria = SpecCriteria.from(SPEC);

        System.out.println("Specification: UC6, manage appointment lifecycle");
        System.out.println("Criteria:      " + criteria.size() + ", read verbatim with their own identifiers");
        System.out.println();
        System.out.println("  " + criteria.get(0).asPrompt().substring(0, Math.min(72,
            criteria.get(0).asPrompt().length())) + "...");
        System.out.println("  " + criteria.size() + " of these. The judge answers all of them.\n");

        System.out.println("--- The code the agent delivered ---\n");
        Judgment delivered = audit(PetClinic.largeCandidate(), criteria, "spec-conformance-uc6");

        para("""
            Three outcomes, and the third one is the honest part. A criterion
            settled by reading a comparison operator is PASS or FAIL. A criterion
            about behaviour that nothing here exercises is neither, and forcing it
            into one would be guessing.

            Watch where the undetermined ones go. They leave the aggregation, so
            the verdict is computed over the criteria that could be settled. That
            is correct, and it is also how a requirement nobody could check stops
            affecting the result. The count is printed for exactly that reason.
            """);

        System.out.println("--- The same code with one operator changed ---\n");
        System.out.println("  !now.isBefore(startTime)   ->   now.isAfter(startTime)\n");
        System.out.println("  One line. All 290 tests still pass: the suite's boundary test");
        System.out.println("  cancels ten minutes late, and the defect lives at zero.\n");

        Judgment seeded = audit(PetClinic.seededCandidate(), criteria, "spec-conformance-uc6-seeded");

        para("""
            That is the negative control, and it is why the first verdict means
            anything. A judge that has only ever been shown working code has not
            been shown to work. This one was given a subject that differs by a
            single character, and the criterion that character governs is the
            criterion that went red.

            It is also the case the tests cannot make. The suite is green in both
            runs. Green tests and an unmet specification are not a contradiction:
            the tests check what somebody thought to write down, and the criteria
            are what was actually asked for.

            And note what this does not tell you: whether the specification was
            right. This answers "did it build what was asked". Module 07 asks
            whether the architecture it established holds up.
            """);

        System.out.println("  delivered  " + delivered.status() + "      seeded  " + seeded.status());
        System.out.println("\nDone.");
    }

    private static Judgment audit(Path workspace, List<SpecCriteria> criteria, String recording) {
        System.out.println("  " + workspace);
        Judgment judgment = CriteriaAuditJudge
            .create("spec-conformance", "Did the implementation satisfy the specification it was built from?",
                workspace, criteria, recording)
            .judge(PetClinic.contextFor(workspace));

        System.out.println("  spec-conformance  " + judgment.status());
        System.out.println("  " + judgment.reasoning() + "\n");

        judgment.checks().stream().filter(check -> !check.passed()).forEach(check -> {
            System.out.println("  FAIL  " + check.name());
            wrap(check.message());
        });
        String undetermined = String.valueOf(judgment.metadata().getOrDefault("undetermined", ""));
        if (!undetermined.isBlank()) {
            System.out.println("\n  could not be determined: " + undetermined);
        }
        return judgment;
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
