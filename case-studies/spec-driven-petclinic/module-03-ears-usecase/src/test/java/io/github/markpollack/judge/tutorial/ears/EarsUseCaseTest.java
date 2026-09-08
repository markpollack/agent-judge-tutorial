package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.junit.JudgeAssertions;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.EarsCriterion;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.EarsJudge;
import io.github.markpollack.judge.ai.requirements.Observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole use case, asserted as a regression contract.
 *
 * <p><b>Not a merge gate</b>, and the distinction matters here more than anywhere. This asserts
 * ABSTAIN and is therefore <em>green</em> — because its subject is the evaluator, and the question is
 * whether replay still reproduces the recorded outcome. A merge gate asks a different question of a
 * different subject: did the implementation meet the bar? That is
 * {@code ShouldIMergeBehaviorDemo}, it requires PASS, and it is red.
 *
 * <p>Note what is <em>not</em> asserted here: {@code assertPass}. Fifty-one of fifty-two
 * requirements were established and the fifty-second could not be, so the specification has not
 * been shown to pass. Asserting PASS would contradict the semantics this module exists to teach.
 */
@DisplayName("Recorded outcome · the complete UC6 specification")
class EarsUseCaseTest {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    @Test
    @DisplayName("Replay contract · 52 requirements → ABSTAIN: UC6-AC41 not established")
    void theCompleteSpecificationCannotBeEstablished() {
        Path workspace = Candidate.workspace();
        JudgeAssertions.assertStatus(JudgmentStatus.ABSTAIN,
            EarsJudge.create("appointment-lifecycle", EarsCriterion.from(CRITERIA), JudgeBackends.forRecording(workspace, "spec-conformance-uc6")),
            Candidate.contextFor(workspace));
    }

    @Test
    @DisplayName("52 checks · 51 established · 1 undetermined · 0 refuted")
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
    @DisplayName("Every criterion answered — none dropped, none invented")
    void everyCriterionIdentifierIsRepresented() {
        Judgment judgment = judge();
        List<String> asked = EarsCriterion.from(CRITERIA).stream().map(EarsCriterion::id).toList();
        List<String> answered = judgment.checks().stream().map(Check::name).toList();

        assertEquals(asked, answered, "answered in the document's order, none dropped, none invented");
    }

    @Test
    @DisplayName("No score: 51 of 52 is a count of outcomes, never 98%")
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
        return EarsJudge.create("appointment-lifecycle", EarsCriterion.from(CRITERIA), JudgeBackends.forRecording(workspace, "spec-conformance-uc6")).judge(Candidate.contextFor(workspace));
    }
}
