/*
 * Module 05: Cascaded Jury
 *
 * Tiered evaluation with CascadedJury. Cheap deterministic checks run first;
 * expensive checks only run if the cheap ones pass. Fail fast, save cost.
 *
 * Run: ./mvnw exec:java -pl module-05-cascaded-jury
 */
package io.github.markpollack.judge.tutorial.module05;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.fs.FileContentJudge;
import io.github.markpollack.judge.fs.FileExistsJudge;
import io.github.markpollack.judge.jury.CascadedJury;
import io.github.markpollack.judge.jury.CompositeAttempt;
import io.github.markpollack.judge.jury.CompositePathEntry;
import io.github.markpollack.judge.jury.CompositePaths;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.jury.TierPolicy;
import io.github.markpollack.judge.jury.Verdict;

public class CascadedJuryDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 05: Cascaded Jury Demo ===\n");

        Path workspace = Path.of("test-workspace");
        String controllerPath = "src/main/java/com/example/HelloController.java";

        JudgmentContext context = JudgmentContext.builder()
            .goal("Add a HelloController class")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        // Tier 1: Cheap deterministic checks (microseconds, free)
        SimpleJury tier1 = SimpleJury.builder()
            .judge(Judges.named(
                new FileExistsJudge(controllerPath),
                "file-exists", "Controller file created"), 1.0)
            .judge(Judges.named(
                new FileExistsJudge("pom.xml"),
                "has-pom", "Maven project exists"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        // Tier 2: Content validation (milliseconds, free)
        SimpleJury tier2 = SimpleJury.builder()
            .judge(Judges.named(
                new FileContentJudge(controllerPath, "hello",
                    FileContentJudge.MatchMode.CONTAINS),
                "has-method", "Contains hello method"), 1.0)
            .judge(Judges.named(
                new FileContentJudge(controllerPath, "class HelloController",
                    FileContentJudge.MatchMode.CONTAINS),
                "correct-class", "Correct class name"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        // Build cascaded jury
        // Tier 1: REJECT_ON_ANY_FAIL — if files don't exist, stop immediately
        // Tier 2: FINAL_TIER — always produces a verdict
        CascadedJury jury = CascadedJury.builder()
            .tier("file-checks", tier1, TierPolicy.REJECT_ON_ANY_FAIL)
            .tier("content-checks", tier2, TierPolicy.FINAL_TIER)
            .build();

        System.out.println("Evaluating with cascaded jury...\n");
        Verdict verdict = jury.vote(context);

        System.out.println("Overall: " + verdict.aggregated().status());
        System.out.println("Reason:  " + verdict.aggregated().reasoning());

        // Show the ordered composite evidence with canonical paths.
        System.out.println("\nComposite attempts (preorder):");
        for (CompositePathEntry entry : CompositePaths.flatten(verdict)) {
            CompositeAttempt attempt = entry.attempt();
            System.out.printf("  %s name=%s relation=%s", entry.path(), attempt.name(),
                attempt.relation().wireName());
            if (attempt.policy() != null) {
                System.out.printf(" policy=%s", attempt.policy().wireName());
            }
            System.out.println();

            if (attempt.verdict() != null) {
                System.out.println("    Status: "
                    + attempt.verdict().aggregated().status());
                attempt.verdict().individualByName().forEach((name, judgment) ->
                    System.out.printf("    %-15s %s%n", name, judgment.status()));
            }
            else {
                System.out.println("    Failure code: "
                    + attempt.failure().code().wireName());
            }
        }

        System.out.println("\nDone.");
    }
}
