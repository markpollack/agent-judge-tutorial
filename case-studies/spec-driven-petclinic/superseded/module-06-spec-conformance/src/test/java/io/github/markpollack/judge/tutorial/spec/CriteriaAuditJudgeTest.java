package io.github.markpollack.judge.tutorial.spec;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModelResponse;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a set of per-requirement answers means.
 *
 * <p>Every case here reads "the answers said X, therefore the judgment must be Y", and never
 * "the code does Y, therefore assert Y". A test written from the code's behaviour cannot
 * disagree with the code, which is how a judge suite ends up asserting its own defect.
 *
 * <p>Note that every assertion is on {@link Judgment#status()}, never {@code pass()}.
 * {@code pass()} is false for FAIL, ERROR and ABSTAIN alike, so a test asserting it is false
 * cannot tell a rejection from a judge that never ran — which is precisely the distinction this
 * judge exists to preserve.
 */
class CriteriaAuditJudgeTest {

    private static final List<Criterion> THREE = List.of(
        new SpecCriteria("UC1-AC1", "first", "The system shall do the first thing."),
        new SpecCriteria("UC1-AC2", "second", "The system shall do the second thing."),
        new SpecCriteria("UC1-AC3", "third", "The system shall do the third thing."));

    @Test
    void everyRequirementSatisfiedIsAPass() {
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals(3, judgment.checks().size(), "every answer is evidence and must be kept");
        assertEquals("3 of 3 requirements pass, 0 fail, 0 could not be determined", judgment.reasoning());
    }

    @Test
    void oneUnsatisfiedRequirementFailsTheWhole() {
        // The definition of done is conjunctive. Two of three is not two-thirds done.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: FAIL - Bar.java:20 compares the wrong way round
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.FAIL, judgment.status());
        assertTrue(judgment.reasoning().contains("1 fail"), judgment.reasoning());
        assertTrue(judgment.checks().stream()
            .anyMatch(check -> check.name().equals("UC1-AC2") && !check.passed()));
        assertTrue(judgment.checks().stream()
            .filter(check -> check.name().equals("UC1-AC2")).findFirst().orElseThrow()
            .message().contains("compares the wrong way round"), "the binding item's evidence survives");
    }

    @Test
    void anUnansweredRequirementIsAnErrorNotAPass() {
        // Answering two of three is not an audit of three. The subject is not at fault
        // here and must not be blamed: the audit is incomplete, which is an ERROR.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            """);

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertTrue(judgment.reasoning().contains("1 of 3"), judgment.reasoning());
        assertTrue(judgment.reasoning().contains("UC1-AC3"), judgment.reasoning());
    }

    @Test
    void nothingDeterminableIsAnAbstentionNotAPass() {
        // The one that matters. Abstentions leave the aggregation population, so a run in
        // which nothing could be settled aggregates over an empty set -- and an empty
        // conjunction is vacuously true. A gate that passes because it lost every
        // requirement is the defect this whole arrangement exists to not have.
        Judgment judgment = judge("""
            UC1-AC1: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC2: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC3: CANNOT_DETERMINE - nothing here exercises it
            """);

        assertEquals(JudgmentStatus.ABSTAIN, judgment.status());
        assertTrue(judgment.reasoning().contains("3 could not be determined"), judgment.reasoning());
    }

    @Test
    void abstentionsDoNotDiluteAFailure() {
        Judgment judgment = judge("""
            UC1-AC1: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC2: FAIL - Bar.java:20 does the opposite
            UC1-AC3: CANNOT_DETERMINE - nothing here exercises it
            """);

        assertEquals(JudgmentStatus.FAIL, judgment.status());
        assertEquals("0 of 3 requirements pass, 1 fail, 2 could not be determined", judgment.reasoning());
    }

    @Test
    void theUndeterminedAreNamedNotJustCounted() {
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals("UC1-AC2", judgment.metadata().get("undetermined"));
        assertEquals(3, judgment.metadata().get("criteriaTotal"));
        assertEquals(2, judgment.metadata().get("determined"),
            "the denominator the verdict was actually computed over");
    }

    @Test
    void anEmptyAuditIsAnErrorNotAPass() {
        assertEquals(JudgmentStatus.ERROR, judge("").status());
        assertEquals(JudgmentStatus.ERROR, judge("   \n  \n").status());
    }

    @Test
    void proseWithoutAnswersIsAnErrorNotAPass() {
        Judgment judgment = judge("I looked at the codebase and everything appears to be in order.");
        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    @Test
    void anAgentThatDidNotFinishIsAnErrorNotAPass() {
        JudgeModel model = request -> new JudgeModelResponse(
            "UC1-AC1: PASS - Foo.java:10 does it", "stub", null, Map.of("successful", false));
        Judgment judgment = CriteriaAuditJudge.create("audit", "d", THREE, model).judge(context());
        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    @Test
    void aMissingRecordingBlamesTheJudgeNotTheSubject() {
        // The recorded backend reports a missing file as an unsuccessful run, which is true of
        // the run and false about the code being audited. An ERROR is right; an ERROR that
        // reads "the agent did not complete" sends the reader to look at the wrong thing.
        JudgeModel model = request -> new JudgeModelResponse(
            "NO_RECORDING", "recorded", null, Map.of("successful", false));
        Judgment judgment = CriteriaAuditJudge.create("audit", "d", THREE, model).judge(context());

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertTrue(judgment.reasoning().contains("No recording to replay"), judgment.reasoning());
    }

    @Test
    void answersForRequirementsNobodyAskedAboutAreIgnored() {
        // An invented identifier must not count towards the roster, or a judge could satisfy
        // the denominator by making things up.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            UC9-AC9: PASS - a criterion nobody wrote
            """);

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertTrue(judgment.reasoning().contains("UC1-AC3"), judgment.reasoning());
    }

    @Test
    void aRepeatedAnswerDoesNotCountTwice() {
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC1: FAIL - changed my mind
            UC1-AC2: PASS - Bar.java:20 does it
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.PASS, judgment.status(), "the first answer stands");
        assertEquals(3, judgment.checks().size());
    }

    @Test
    void answersOutOfOrderAreStillAnswers() {
        // The real agent emitted AC1-AC46, then AC48-AC52, then AC47. Order is not
        // part of the contract; completeness is.
        Judgment judgment = judge("""
            UC1-AC3: PASS - Baz.java:30 does it
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            """);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals(List.of("UC1-AC1", "UC1-AC2", "UC1-AC3"),
            judgment.checks().stream().map(check -> check.name()).toList(),
            "reported in the document's order, not the agent's");
    }

    @Test
    void aVerdictNeverCarriesAFailingCheck() {
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: FAIL - Bar.java:20 does the opposite
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertFalse(judgment.checks().stream().allMatch(check -> check.passed()));
        assertEquals(JudgmentStatus.FAIL, judgment.status(),
            "a judgment carrying a failing check must not be a pass");
    }

    private static Judgment judge(String answers) {
        JudgeModel model = request -> new JudgeModelResponse(answers, "stub", null, Map.of());
        return CriteriaAuditJudge.create("audit", "d", THREE, model).judge(context());
    }

    private static JudgmentContext context() {
        return JudgmentContext.builder()
            .goal("audit the requirements")
            .workspace(Path.of("."))
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(1))
            .build();
    }
}
