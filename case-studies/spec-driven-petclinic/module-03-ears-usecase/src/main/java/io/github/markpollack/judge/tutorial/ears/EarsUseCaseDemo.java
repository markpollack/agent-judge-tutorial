/*
 * Module 03: Run the whole spec.
 *
 * Six requirements were readable, and all six passed. The specification has 52,
 * and completeness changes the conclusion.
 *
 * Same judge. Same document. Nothing new to learn except what happens when you
 * stop sampling.
 *
 * Run:               ./mvnw exec:java -pl module-03-ears-usecase
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.EarsCriterion;
import io.github.markpollack.judge.tutorial.support.EarsJudge;
import io.github.markpollack.judge.tutorial.support.Observation;

public class EarsUseCaseDemo {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    public static void main(String[] args) {
        System.out.println("=== Module 03: Run the whole spec ===\n");
        System.out.println("The six we sampled passed.");
        System.out.println("Now run the complete use-case specification.\n");

        List<EarsCriterion> criteria = EarsCriterion.from(CRITERIA);
        Path workspace = Candidate.workspace();

        System.out.println("  " + criteria.size() + " required criteria\n");

        Judgment judgment = EarsJudge
            .create("appointment-lifecycle", workspace, criteria, "spec-conformance-uc6")
            .judge(Candidate.contextFor(workspace));

        Map<String, EarsCriterion> byId = criteria.stream()
            .collect(java.util.stream.Collectors.toMap(EarsCriterion::id, c -> c, (a, b) -> a));
        List<String> unestablished = unestablished(judgment);
        long established = judgment.checks().stream().filter(Check::passed).count();
        long refuted = judgment.checks().size() - established - unestablished.size();

        System.out.printf("  %2d PASS%n", established);
        System.out.printf("  %2d FAIL%n", refuted);
        System.out.printf("  %2d ABSTAIN%n%n", unestablished.size());
        System.out.println("  Overall: " + judgment.status() + "\n");

        if (!unestablished.isEmpty()) {
            System.out.println("  Cannot establish:");
            unestablished.forEach(id -> System.out.println("    " + id + "  "
                + (byId.containsKey(id) ? byId.get(id).title() : "")));
            System.out.println();
        }

        int observations = Observation.of(judgment).size();
        if (observations > 0) {
            System.out.println("  Additional observations recorded: " + observations + "\n");
        }

        para("""
            Almost all is not the same as done.

            PASS means I established every required criterion. I don't turn
            51 out of 52 into 98% and call it done.
            """);
        System.out.println("Done.");
    }

    /** The criteria the audit could not settle. Not a failure of the code — a gap in the evidence. */
    private static List<String> unestablished(Judgment judgment) {
        Object stored = judgment.metadata().get("unestablished");
        String ids = stored == null ? "" : stored.toString();
        return ids.isBlank() ? List.of() : List.of(ids.split(","));
    }

    private static void para(String text) {
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
