package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.junit.JudgeAssertions;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.EarsCriterion;
import io.github.markpollack.judge.tutorial.support.EarsJudge;
import io.github.markpollack.judge.tutorial.support.Observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole use case, asserted as a gate.
 *
 * <p>Note what is <em>not</em> asserted here: {@code assertPass}. Fifty-one of fifty-two
 * requirements were established and the fifty-second could not be, so the specification has not
 * been shown to pass. Asserting PASS would contradict the semantics this module exists to teach.
 */
class EarsUseCaseTest {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    @Test
    void theCompleteSpecificationCannotBeEstablished() {
        Path workspace = Candidate.workspace();
        JudgeAssertions.assertStatus(JudgmentStatus.ABSTAIN,
            EarsJudge.create("appointment-lifecycle", workspace, EarsCriterion.from(CRITERIA),
                "spec-conformance-uc6"),
            Candidate.contextFor(workspace));
    }

    @Test
    void theRosterIsCompleteAndTheOutcomeIsNotAccidental() {
        Judgment judgment = judge();

        assertEquals(52, EarsCriterion.from(CRITERIA).size(), "the parser is the denominator");
        assertEquals(52, judgment.checks().size(), "one check per required criterion, all retained");
        assertEquals(52, judgment.metadata().get("criteriaTotal"));

        long established = judgment.checks().stream().filter(Check::passed).count();
        List<String> unestablished = List.of(judgment.metadata().get("unestablished").toString().split(","));

        assertEquals(51L, established, "51 established");
        assertEquals(1, unestablished.size(), "1 could not be established");
        assertEquals(0L, judgment.checks().size() - established - unestablished.size(), "0 refuted");
        assertEquals(List.of("UC6-AC41"), unestablished, "UC6-AC41 is the only one");
    }

    @Test
    void everyCriterionIdentifierIsRepresented() {
        Judgment judgment = judge();
        List<String> asked = EarsCriterion.from(CRITERIA).stream().map(EarsCriterion::id).toList();
        List<String> answered = judgment.checks().stream().map(Check::name).toList();

        assertEquals(asked, answered, "answered in the document's order, none dropped, none invented");
    }

    @Test
    void noScoreAndNoObservationInfluencesTheStatus() {
        Judgment judgment = judge();

        assertNull(judgment.score(), "51 of 52 is a count of outcomes, never a rating");
        assertTrue(judgment.reasoning().contains("UC6-AC41"), judgment.reasoning());
        // Observations, if the recording carries any, sit outside the rollup entirely.
        assertEquals(JudgmentStatus.ABSTAIN, judgment.status());
        assertTrue(Observation.of(judgment).stream().noneMatch(o -> o.requirementId().isBlank()));
    }

    private static Judgment judge() {
        Path workspace = Candidate.workspace();
        return EarsJudge.create("appointment-lifecycle", workspace, EarsCriterion.from(CRITERIA),
            "spec-conformance-uc6").judge(Candidate.contextFor(workspace));
    }
}
