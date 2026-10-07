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

import io.github.markpollack.judge.judgment.JudgmentStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.judgment.Check;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.EarsRequirement;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.EarsJury;
import io.github.markpollack.judge.verdict.Verdict;
import io.github.markpollack.judge.jury.Jury;
import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.tutorial.support.RosterResults;
import io.github.markpollack.judge.ai.requirements.Observation;

public class EarsUseCaseDemo {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");


    /** Configure one whole-roster investigation. Workspace/tools belong to the runtime. */
    public static Jury jury(EvalModel runtime) {
        return EarsJury.builder()
            .runtime(runtime)
            .requirements(EarsRequirement.from(CRITERIA, "petclinic:fc9df4af"))
            .build();
    }

    public static void main(String[] args) {
        System.out.println("=== Module 03: Run the whole spec ===\n");
        System.out.println("The six we sampled passed.");
        System.out.println("Now run the complete use-case specification.\n");

        List<EarsRequirement> criteria = EarsRequirement.from(CRITERIA, "petclinic:fc9df4af");
        Path workspace = Candidate.workspace();

        System.out.println("  " + criteria.size() + " required criteria\n");

        Verdict verdict = jury(JudgeBackends.forRecording(workspace, "spec-conformance-uc6")).vote();
        Judgment judgment = verdict.judgment();

        Map<String, EarsRequirement> byId = criteria.stream()
            .collect(java.util.stream.Collectors.toMap(EarsRequirement::id, c -> c, (a, b) -> a));
        List<String> unestablished = RosterResults.checks(verdict).stream().filter(c -> c.judgment().status() == JudgmentStatus.ABSTAIN).map(Check::id).toList();
        long established = RosterResults.checks(verdict).stream().filter(c -> c.judgment().status() == JudgmentStatus.PASS).count();
        long refuted = RosterResults.checks(verdict).stream().filter(c -> c.judgment().status() == JudgmentStatus.FAIL).count();

        System.out.printf("  %2d PASS%n", established);
        System.out.printf("  %2d FAIL%n", refuted);
        System.out.printf("  %2d ABSTAIN%n%n", unestablished.size());
        System.out.println("  Overall: " + verdict.conclusion() + "\n");

        if (!unestablished.isEmpty()) {
            System.out.println("  Cannot establish:");
            unestablished.forEach(id -> System.out.println("    " + id + "  "
                + (byId.containsKey(id) ? byId.get(id).specification().title() : "")));
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

    private static void para(String text) {
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
