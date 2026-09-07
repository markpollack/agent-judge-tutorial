package io.github.markpollack.judge.tutorial.support;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a set of per-criterion answers means.
 *
 * <p>Every case reads "the answers said X, therefore the judgment must be Y" — written from the
 * rubric, never from the code. A test written from the code's behaviour cannot disagree with it.
 *
 * <p>Every assertion is on {@link Judgment#status()}, never {@code pass()}. {@code pass()} is false
 * for FAIL, ERROR and ABSTAIN alike, so a test asserting it is false cannot tell a rejection from a
 * judge that never ran.
 */
class EarsJudgeTest {

    private static final List<EarsCriterion> THREE = List.of(
        new EarsCriterion("UC1-AC1", "first", "When a thing happens, the system shall do the first thing."),
        new EarsCriterion("UC1-AC2", "second", "If a thing happens, then the system shall do the second thing."),
        new EarsCriterion("UC1-AC3", "third", "While a state holds, the system shall do the third thing."));

    @Test
    void everyRequirementEstablishedIsAPass() {
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals(3, judgment.checks().size(), "every answer is evidence and must be kept");
        assertEquals("all 3 requirements established", judgment.reasoning());
    }

    @Test
    void oneUnsatisfiedRequirementFailsTheWhole() {
        // Required criteria are conjunctive. Two of three is not two-thirds done.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: FAIL - Bar.java:20 compares the wrong way round
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.FAIL, judgment.status());
        assertTrue(judgment.checks().stream()
            .anyMatch(c -> c.name().equals("UC1-AC2") && !c.passed()));
        assertTrue(judgment.checks().stream()
            .filter(c -> c.name().equals("UC1-AC2")).findFirst().orElseThrow()
            .message().contains("compares the wrong way round"), "the binding item's evidence survives");
    }

    @Test
    void oneAbstentionMakesTheWholeAbstain() {
        // The load-bearing rule. A written acceptance criterion is required by construction:
        // the specification says it applies. So CANNOT_DETERMINE means "could not establish",
        // not "does not apply", and it must not be absorbed into a passing population.
        // PASS means every required criterion was affirmatively established.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.ABSTAIN, judgment.status(),
            "51 of 52 established is not the specification passing");
        assertTrue(judgment.reasoning().contains("UC1-AC2"), judgment.reasoning());
        assertEquals("UC1-AC2", judgment.metadata().get("unestablished"));
    }

    @Test
    void aFailureOutranksAnAbstention() {
        Judgment judgment = judge("""
            UC1-AC1: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC2: FAIL - Bar.java:20 does the opposite
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void nothingEstablishedIsAnAbstentionNotAPass() {
        Judgment judgment = judge("""
            UC1-AC1: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC2: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC3: CANNOT_DETERMINE - nothing here exercises it
            """);

        assertEquals(JudgmentStatus.ABSTAIN, judgment.status());
    }

    @Test
    void anUnansweredRequirementIsAnErrorNotAPass() {
        // Answering two of three is not an audit of three. The subject is not at fault:
        // the audit is incomplete, which is an ERROR.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            """);

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertTrue(judgment.reasoning().contains("1 of 3"), judgment.reasoning());
        assertTrue(judgment.reasoning().contains("UC1-AC3"), judgment.reasoning());
    }

    @Test
    void answersForRequirementsNobodyAskedAboutAreIgnored() {
        // An invented identifier must not satisfy the roster.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            UC9-AC9: PASS - a criterion nobody wrote
            """);

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertTrue(judgment.reasoning().contains("UC1-AC3"), judgment.reasoning());
    }

    @Test
    void answersOutOfOrderAreStillAnswers() {
        // The real agent emitted AC1-AC46, then AC48-AC52, then AC47. Order is not part of
        // the contract; completeness is.
        Judgment judgment = judge("""
            UC1-AC3: PASS - Baz.java:30 does it
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: PASS - Bar.java:20 does it
            """);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals(List.of("UC1-AC1", "UC1-AC2", "UC1-AC3"),
            judgment.checks().stream().map(c -> c.name()).toList(),
            "reported in the document's order, not the agent's");
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
    void anEmptyAuditIsAnErrorNotAPass() {
        assertEquals(JudgmentStatus.ERROR, judge("").status());
        assertEquals(JudgmentStatus.ERROR, judge("   \n  \n").status());
        assertEquals(JudgmentStatus.ERROR,
            judge("I looked at the codebase and everything appears to be in order.").status());
    }

    @Test
    void aMissingRecordingBlamesTheJudgeNotTheSubject() {
        // ERROR is right; an ERROR reading "the agent did not complete" sends the reader to
        // look at the wrong thing.
        JudgeModel model = request -> new JudgeModelResponse(
            RecordedJudgeModel.NO_RECORDING, "recorded", null, Map.of("successful", false));
        Judgment judgment = EarsJudge.create("audit", THREE, model).judge(context());

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertTrue(judgment.reasoning().contains("No recording to replay"), judgment.reasoning());
    }

    @Test
    void noNumericScoreAppearsAnywhere() {
        // DD-12. A score of 6 out of 10 is meaningless if you do not know what makes it 7.
        Judgment judgment = judge("""
            UC1-AC1: PASS - Foo.java:10 does it
            UC1-AC2: CANNOT_DETERMINE - nothing here exercises it
            UC1-AC3: PASS - Baz.java:30 does it
            """);

        assertNull(judgment.score(), "no score is set, because none is meaningful here");
        assertTrue(judgment.reasoning().matches(".*\\d+ of \\d+ established.*"),
            "the report counts requirements; it does not rate them");
    }

    private static Judgment judge(String answers) {
        JudgeModel model = request -> new JudgeModelResponse(answers, "stub", null, Map.of());
        return EarsJudge.create("audit", THREE, model).judge(context());
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
