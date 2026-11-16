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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.coverage.CoverageComparison;
import io.github.markpollack.judge.coverage.CoveragePreservationJudge;
import io.github.markpollack.judge.file.FileContentJudge;
import io.github.markpollack.judge.file.FileExistsJudge;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.jury.CascadedJury;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.verdict.CompositeAttempt;
import io.github.markpollack.judge.verdict.CompositePathEntry;
import io.github.markpollack.judge.verdict.CompositePaths;
import io.github.markpollack.judge.verdict.RoutingRule;
import io.github.markpollack.judge.verdict.Verdict;
import io.github.markpollack.judge.voting.ErrorHandling;
import io.github.markpollack.judge.voting.MajorityVotingStrategy;
import io.github.markpollack.judge.voting.TieBreakRule;

public class ErrorAndEscalationDemo {

    private static final Path WORKSPACE = Path.of("test-workspace");

    public static void main(String[] args) throws Exception {
        System.out.println("=== Module 09: Error, abstention, and escalation ===\n");

        // A workspace where no build ever ran. The coverage judge cannot
        // complete here, and this is a real ERROR rather than a staged one:
        // it is the same judge module 03 used, on a directory with no report.
        Path neverBuilt = Files.createTempDirectory("no-build-ran");

        // build and tests stand in here. The subject of this module is what the
        // jury does with the third judge, so the other two are kept trivial.
        Judge build = Judges.named(() -> Judgment.pass("exit code 0"), "build");
        Judge tests = Judges.named(() -> Judgment.pass("4 tests, 0 failures"), "tests");
        Judge coverage = Judges.named(CoveragePreservationJudge.builder(5.0)
            .evidence(new CoverageComparison(neverBuilt, 100.0)).build(), "coverage");

        System.out.println("--- Three judges, one of which cannot run ---\n");
        for (Judge judge : new Judge[] { build, tests, coverage }) {
            Judgment judgment = judge.judge();
            System.out.printf("  %-10s %s%n",
                Judges.tryMetadata(judge).map(m -> m.name()).orElse("?"), judgment.status());
            wrap(judgment.reasoning());
        }

        // ---------------------------------------------------------------
        // The same panel, under each error policy.
        // ---------------------------------------------------------------
        System.out.println("\n--- The same panel, under each ErrorHandling ---\n");
        System.out.printf("  %-18s %-8s %s%n", "POLICY", "STATUS", "ROSTER");
        System.out.printf("  %-18s %-8s %s%n", "-".repeat(18), "------", "----------");

        for (ErrorHandling policy : ErrorHandling.values()) {
            SimpleJury jury = SimpleJury.builder()
                .judge(build, 1.0)
                .judge(tests, 1.0)
                .judge(coverage, 1.0)
                .votingStrategy(new MajorityVotingStrategy(TieBreakRule.FAIL, policy))
                .build();

            Verdict verdict = jury.vote();
            Map<String, Object> evidence = aggregation(verdict.judgment());
            System.out.printf("  %-18s %-8s %s of %s%n",
                policy, verdict.judgment().status(),
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
            .votingStrategy(new MajorityVotingStrategy(TieBreakRule.FAIL, ErrorHandling.IGNORE))
            .build();
        Verdict ignored = ignoring.vote();

        System.out.println("  aggregate status: " + ignored.judgment().status());
        System.out.println("  named members:    " + ignored.individualByName().keySet());
        Map<String, Object> evidence = aggregation(ignored.judgment());
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
        // CoveragePreservationJudge cannot even be built without a baseline:
        // CoverageComparison requires one. So the question moves to whoever
        // supplies the evidence, and this judge is that caller. With no recorded
        // baseline there is nothing to measure a drop against, so it abstains
        // rather than guess.
        Judgment abstained = coverageAgainst(Optional.empty()).judge();
        System.out.println("  status:    " + abstained.status());
        System.out.println("  reasoning: " + abstained.reasoning());
        System.out.println("  pass():    " + abstained.pass());

        para("""
            pass() is false for FAIL, ERROR, ABSTAIN and NOT_APPLICABLE alike.
            A consumer that branches on !pass() records this as a rejected subject,
            when in fact nothing was ever asked. Assert status(), not pass().
            """);

        System.out.println("--- Inapplicability is a different answer ---\n");
        Judgment excluded = Judgment.notApplicable("This subject has no Java sources to assess");
        System.out.println("  status:    " + excluded.status());
        System.out.println("  reasoning: " + excluded.reasoning());
        System.out.println("  pass():    " + excluded.pass());
        para("""
            ABSTAIN: an applicable question was left undecided.
            NOT_APPLICABLE: the criterion does not apply to this subject.
            A judge seated in a jury must declare when it can be inapplicable;
            an undeclared exclusion becomes ERROR, not permission to skip a rule.
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
        SimpleJury structural = SimpleJury.builder()
            .judge(Judges.named(FileExistsJudge.builder(controller).evidence(WORKSPACE).build(),
                "file-exists"), 1.0)
            .judge(Judges.named(FileExistsJudge.builder("pom.xml").evidence(WORKSPACE).build(),
                "has-pom"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        SimpleJury content = SimpleJury.builder()
            .judge(Judges.named(FileContentJudge.builder(controller, "class ReportController",
                FileContentJudge.MatchMode.CONTAINS).evidence(WORKSPACE).build(), "correct-class"), 1.0)
            .judge(Judges.named(FileContentJudge.builder(controller, "@RestController",
                FileContentJudge.MatchMode.CONTAINS).evidence(WORKSPACE).build(), "is-annotated"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        CascadedJury jury = CascadedJury.builder()
            .tier("structural", structural, RoutingRule.STOP_ON_ANY_OPINION_FAIL)
            .tier("content", content, RoutingRule.FINAL_TIER)
            .build();

        Verdict verdict = jury.vote();
        System.out.println("  overall: " + verdict.judgment().status());
        System.out.println("  reason:  " + verdict.judgment().reasoning());

        // The composite evidence records which tiers ran and which did not, so a
        // subject rejected at tier 1 is distinguishable from one that failed at
        // tier 2. "It stopped early" is itself a finding.
        System.out.println("\n  what actually ran:");
        for (CompositePathEntry entry : CompositePaths.flatten(verdict)) {
            CompositeAttempt attempt = entry.attempt();
            System.out.printf("    %-28s %s", entry.path(),
                attempt.routingRule() != null ? attempt.routingRule().wireName() : attempt.relation().wireName());
            if (attempt.verdict() != null) {
                System.out.print("  " + attempt.verdict().judgment().status());
            }
            else {
                System.out.print("  not run: " + attempt.failure().code().wireName());
            }
            System.out.println();
        }
    }

    /**
     * A coverage gate whose caller may not have a baseline. Without one there is
     * nothing to compare against, so the honest answer is ABSTAIN - the question
     * was not decided - rather than a FAIL that blames the subject.
     */
    private static Judge coverageAgainst(Optional<Double> baseline) {
        return Judges.named(() -> baseline
            .map(recorded -> CoveragePreservationJudge.builder(5.0)
                .evidence(new CoverageComparison(WORKSPACE, recorded)).build().judge())
            .orElseGet(() -> Judgment.abstain(
                "No baseline coverage was recorded, so a coverage drop cannot be measured")),
            "coverage");
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
