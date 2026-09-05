/*
 * Module 08: When aggregation is the right answer
 *
 * Module 06 refused to aggregate: five criteria, five different questions,
 * every one of them required. Voting on those would let "the tests pass"
 * outvote "it does not compile".
 *
 * This is the other case, and it is the one a jury is for:
 *
 *     one uncertain property, several independent estimates of it.
 *
 *     Compose requirements. Aggregate estimates of the same property.
 *
 * Run: ./mvnw exec:java -pl module-08-jury
 */
package io.github.markpollack.judge.tutorial.module08;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.jury.ConsensusStrategy;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.jury.Verdict;
import io.github.markpollack.judge.result.Judgment;

public class JuryDemo {

    private static final String QUESTION =
        "Does ReportController fit this codebase's conventions and idioms?";

    public static void main(String[] args) {
        System.out.println("=== Module 08: When aggregation is the right answer ===\n");
        System.out.println("  The question, asked three times:");
        System.out.println("  \"" + QUESTION + "\"\n");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(Path.of("test-workspace"))
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .build();

        // Three reviewers of the same property, prompted to weigh different
        // things. In production each is a ModelBackedJudge with its own prompt
        // — see module 07. Their opinions are recorded here so the module runs
        // with no credentials, and so the panel reliably disagrees.
        Judge layering = reviewer("reviewer-a", false,
            "reaches the database directly, unlike its neighbours");
        Judge simplicity = reviewer("reviewer-b", false,
            "hand-rolled SQL, JSON and error handling");
        Judge consistency = reviewer("reviewer-c", true,
            "package, class name and method shape all match");

        System.out.println("  reviewer-a  (weighs layering)     " + status(layering, context));
        System.out.println("  reviewer-b  (weighs simplicity)   " + status(simplicity, context));
        System.out.println("  reviewer-c  (weighs consistency)  " + status(consistency, context));

        // ---------------------------------------------------------------
        // Majority: the panel's best estimate.
        // ---------------------------------------------------------------
        System.out.println("\n--- Majority ---\n");
        Verdict majority = jury(new MajorityVotingStrategy(), layering, simplicity, consistency)
            .vote(context);
        System.out.println("  " + majority.aggregated().status()
            + " — " + majority.aggregated().reasoning());

        para("""
            This is a legitimate aggregate. All three judges were estimating the
            same uncertain quantity, so combining them says something the
            individual answers did not: two of three independent reviewers found
            the same problem.
            """);

        // ---------------------------------------------------------------
        // Consensus: a split panel has no collective finding.
        // ---------------------------------------------------------------
        System.out.println("--- Consensus ---\n");
        Verdict consensus = jury(new ConsensusStrategy(), layering, simplicity, consistency)
            .vote(context);
        System.out.println("  " + consensus.aggregated().status()
            + " — " + consensus.aggregated().reasoning());

        para("""
            ABSTAIN, not FAIL. A split vote is indeterminate, not negative: the
            panel disagreed, which is a different fact from the panel rejecting
            the subject, and it usually calls for a human rather than a merge
            button. Rejection belongs to the gate, not to the aggregation step.
            """);

        // ---------------------------------------------------------------
        // What the aggregate cost, and what survived it.
        // ---------------------------------------------------------------
        System.out.println("--- The estimates survive the aggregate ---\n");
        majority.individualByName().forEach((name, judgment) -> {
            System.out.printf("  %-12s %-6s %s%n", name, judgment.status(), judgment.reasoning());
        });
        System.out.println("\n  weights: " + majority.weights());
        para("""
            The dissent is still there. reviewer-c said PASS, and a verdict that
            recorded only "FAIL, 2 of 3" could not tell you who disagreed or why
            — which is the first thing worth reading when a panel splits.

            Note the key space, though: weights are keyed by the judge's position
            in the builder, while individualByName() is keyed by its name. Join
            them by order, not by name.
            """);

        // ---------------------------------------------------------------
        // The strategy we are deliberately not using.
        // ---------------------------------------------------------------
        System.out.println("--- What this module does not do ---");
        para("""
            There is no weighted average here, and its absence is the point.

            These three judges made no measurement. Averaging them means reading
            PASS as 1.0 and FAIL as 0.0 and calling the result 0.33 — a number
            invented by the aggregation step, carrying a precision nobody
            measured, and now comparable against thresholds nobody derived.

            Use AverageVotingStrategy, WeightedAverageStrategy or
            MedianVotingStrategy when the judges genuinely produce scores, as
            module 03's coverage judge does. Weight one reviewer above another
            only when you can say what the weight is for.
            """);

        System.out.println("Done.");
    }

    private static SimpleJury jury(io.github.markpollack.judge.jury.VotingStrategy strategy, Judge... judges) {
        SimpleJury.Builder builder = SimpleJury.builder().votingStrategy(strategy);
        for (Judge judge : judges) {
            builder.judge(judge, 1.0);
        }
        return builder.build();
    }

    private static Judge reviewer(String name, boolean fits, String reasoning) {
        return Judges.named(context -> Judgment.verdict(fits).reasoning(reasoning).build(),
            name, "Independent estimate of architectural fit");
    }

    private static String status(Judge judge, JudgmentContext context) {
        return judge.judge(context).status().toString();
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
