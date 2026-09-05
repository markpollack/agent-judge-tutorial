/*
 * Module 09: Error, abstention, and escalation
 *
 * One rule, in the same shape as the one you already trust from testing:
 *
 *     A judge that failed, timed out, was never configured, or declined to
 *     answer must not make the result look better.
 *
 * A broken test fails closed - it goes red and you fix it. A broken judge
 * fails open: it returns a plausible number and you build on it. Nothing
 * checks the checker, so the fail-closed behaviour has to be chosen.
 *
 * Run: ./mvnw exec:java -pl module-09-error-and-escalation
 */
package io.github.markpollack.judge.tutorial.module09;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.coverage.CoveragePreservationJudge;
import io.github.markpollack.judge.fs.FileContentJudge;
import io.github.markpollack.judge.fs.FileExistsJudge;
import io.github.markpollack.judge.jury.CascadedJury;
import io.github.markpollack.judge.jury.CompositeAttempt;
import io.github.markpollack.judge.jury.CompositePathEntry;
import io.github.markpollack.judge.jury.CompositePaths;
import io.github.markpollack.judge.jury.ErrorPolicy;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.jury.TiePolicy;
import io.github.markpollack.judge.jury.Verdict;
import io.github.markpollack.judge.result.Judgment;

public class ErrorAndEscalationDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    public static void main(String[] args) throws Exception {
        System.out.println("=== Module 09: Error, abstention, and escalation ===\n");

        // A workspace where no build ever ran. The coverage judge cannot
        // complete here, and this is a real ERROR rather than a staged one:
        // it is the same judge module 03 used, on a directory with no report.
        Path neverBuilt = Files.createTempDirectory("no-build-ran");

        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(neverBuilt)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .metadata("baselineCoverage", 100.0)
            .build();

        // build and tests stand in here. The subject of this module is what the
        // jury does with the third judge, so the other two are kept trivial.
        Judge build = Judges.named(ctx -> Judgment.pass("exit code 0"), "build");
        Judge tests = Judges.named(ctx -> Judgment.pass("4 tests, 0 failures"), "tests");
        Judge coverage = Judges.named(new CoveragePreservationJudge(5.0), "coverage");

        System.out.println("--- Three judges, one of which cannot run ---\n");
        for (Judge judge : new Judge[] { build, tests, coverage }) {
            Judgment judgment = judge.judge(context);
            System.out.printf("  %-10s %s%n",
                Judges.tryMetadata(judge).map(m -> m.name()).orElse("?"), judgment.status());
            wrap(judgment.reasoning());
        }

        // ---------------------------------------------------------------
        // The same panel, under each error policy.
        // ---------------------------------------------------------------
        System.out.println("\n--- The same panel, under each ErrorPolicy ---\n");
        System.out.printf("  %-18s %-8s %s%n", "POLICY", "STATUS", "ROSTER");
        System.out.printf("  %-18s %-8s %s%n", "-".repeat(18), "------", "----------");

        for (ErrorPolicy policy : ErrorPolicy.values()) {
            SimpleJury jury = SimpleJury.builder()
                .judge(build, 1.0)
                .judge(tests, 1.0)
                .judge(coverage, 1.0)
                .votingStrategy(new MajorityVotingStrategy(TiePolicy.FAIL, policy))
                .build();

            Verdict verdict = jury.vote(context);
            Map<String, Object> evidence = aggregation(verdict.aggregated());
            System.out.printf("  %-18s %-8s %s of %s%n",
                policy, verdict.aggregated().status(),
                evidence.getOrDefault("eligibleCount", "?"),
                evidence.getOrDefault("inputCount", "?"));
        }

        para("""
            Three of those four report PASS for a run in which a required
            criterion was never evaluated at all.

            TREAT_AS_FAIL is the surprise. It did convert the error into a
            failure, the roster is intact at 3 of 3, and the verdict is still
            PASS - because majority voting is compensatory, and two passes
            outvote one failure. The error policy and the aggregation rule are
            two separate decisions, and getting one of them right does not save
            you from the other. This is module 06's point arriving from the other
            direction.

            Only PROPAGATE fails closed here, and it is the default. That is the
            one piece of luck in this table. Choose the policy on purpose rather
            than inheriting it.
            """);

        // ---------------------------------------------------------------
        // The roster is the tell.
        // ---------------------------------------------------------------
        System.out.println("--- The roster is the tell ---\n");
        SimpleJury ignoring = SimpleJury.builder()
            .judge(build, 1.0)
            .judge(tests, 1.0)
            .judge(coverage, 1.0)
            .votingStrategy(new MajorityVotingStrategy(TiePolicy.FAIL, ErrorPolicy.IGNORE))
            .build();
        Verdict ignored = ignoring.vote(context);

        System.out.println("  aggregate status: " + ignored.aggregated().status());
        System.out.println("  named members:    " + ignored.individualByName().keySet());
        Map<String, Object> evidence = aggregation(ignored.aggregated());
        System.out.println("  inputCount:       " + evidence.get("inputCount"));
        System.out.println("  eligibleCount:    " + evidence.get("eligibleCount"));
        System.out.println("  errorCount:       " + evidence.get("errorCount"));
        System.out.println("  ignoredErrorCount:" + evidence.get("ignoredErrorCount"));

        para("""
            The verdict still names all three judges, and the aggregate was
            computed from two. Nothing here is hidden - inputCount and
            eligibleCount are both recorded - but nothing raises its voice either.

            So assert the roster: eligibleCount == inputCount, wherever you expect
            a full panel. That single equality is the difference between a jury
            that scored what you asked for and one that scored what survived.
            """);

        // ---------------------------------------------------------------
        // Abstention is not agreement.
        // ---------------------------------------------------------------
        System.out.println("--- Abstention is not agreement ---\n");
        JudgmentContext noBaseline = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(WORKSPACE)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .build();

        Judgment abstained = new CoveragePreservationJudge(5.0).judge(noBaseline);
        System.out.println("  status:    " + abstained.status());
        System.out.println("  reasoning: " + abstained.reasoning());
        System.out.println("  pass():    " + abstained.pass());

        para("""
            pass() is false - and it is false for FAIL, ERROR and ABSTAIN alike.
            A consumer that branches on !pass() records this as a rejected subject,
            when in fact nothing was ever asked. Assert status(), not pass().
            """);

        // ---------------------------------------------------------------
        // Escalation: stop early, and say where you stopped.
        // ---------------------------------------------------------------
        System.out.println("--- Escalation: spend nothing on a subject already rejected ---\n");
        System.out.println("  a subject that survives the cheap tier:");
        cascade("src/main/java/com/example/ReportController.java");
        System.out.println("\n  a subject that does not:");
        cascade("src/main/java/com/example/NoSuchController.java");
        para("""
            The second subject never reached the content tier: only /structural
            appears in the composite evidence. Which tiers are present is itself
            the record, so "rejected at tier 1" stays distinguishable from
            "failed at tier 2" - and a tier that never ran is never mistaken for
            a tier that passed.

            A cascade is cost control. It is not an evaluation policy, and it
            does not change what any judge would have decided.
            """);

        Files.deleteIfExists(neverBuilt);
        System.out.println("Done.");
    }

    /**
     * A cascade is cost control, not evaluation logic. Cheap deterministic checks
     * run first; the expensive tier only runs on subjects that survived.
     */
    private static void cascade(String controller) {
        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(WORKSPACE)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        SimpleJury structural = SimpleJury.builder()
            .judge(Judges.named(new FileExistsJudge(controller), "file-exists"), 1.0)
            .judge(Judges.named(new FileExistsJudge("pom.xml"), "has-pom"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        SimpleJury content = SimpleJury.builder()
            .judge(Judges.named(new FileContentJudge(controller, "class ReportController",
                FileContentJudge.MatchMode.CONTAINS), "correct-class"), 1.0)
            .judge(Judges.named(new FileContentJudge(controller, "@RestController",
                FileContentJudge.MatchMode.CONTAINS), "is-annotated"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        CascadedJury jury = CascadedJury.builder()
            .tier("structural", structural, io.github.markpollack.judge.jury.TierPolicy.REJECT_ON_ANY_FAIL)
            .tier("content", content, io.github.markpollack.judge.jury.TierPolicy.FINAL_TIER)
            .build();

        Verdict verdict = jury.vote(context);
        System.out.println("  overall: " + verdict.aggregated().status());
        System.out.println("  reason:  " + verdict.aggregated().reasoning());

        // The composite evidence records which tiers ran and which did not, so a
        // subject rejected at tier 1 is distinguishable from one that failed at
        // tier 2. "It stopped early" is itself a finding.
        System.out.println("\n  what actually ran:");
        for (CompositePathEntry entry : CompositePaths.flatten(verdict)) {
            CompositeAttempt attempt = entry.attempt();
            System.out.printf("    %-28s %s", entry.path(),
                attempt.policy() != null ? attempt.policy().wireName() : attempt.relation().wireName());
            if (attempt.verdict() != null) {
                System.out.print("  " + attempt.verdict().aggregated().status());
            }
            else {
                System.out.print("  not run: " + attempt.failure().code().wireName());
            }
            System.out.println();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> aggregation(Judgment judgment) {
        Object block = judgment.metadata().get(Judgment.AGGREGATION_KEY);
        return block instanceof Map<?, ?> map ? new LinkedHashMap<>((Map<String, Object>) map) : Map.of();
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
