package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.junit.JudgeAssertions;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.EarsCriterion;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.EarsJudge;

import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.ai.requirements.Observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same judge the demo runs, as a gate.
 *
 * <p>Replays the committed recording, so it is offline, deterministic and free. What a recorded
 * run verifies is the wiring and the semantics — never that the judge is right.
 */
@DisplayName("Module 02 · six EARS requirements, read from UC6")
class EarsSliceTest {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    private static final String[] SLICE =
        { "UC6-AC7", "UC6-AC8", "UC6-AC9", "UC6-AC10", "UC6-AC11", "UC6-AC12" };

    @Test
    @DisplayName("6 requirements → PASS")
    void theCancellationSliceIsFullyEstablished() {
        List<EarsCriterion> slice = EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE);
        Path workspace = Candidate.workspace();

        JudgeAssertions.assertStatus(JudgmentStatus.PASS,
            EarsJudge.create("appointment-cancellation", slice, JudgeBackends.forRecording(workspace, "ears-uc6-cancellation")),
            Candidate.contextFor(workspace));
    }

    @Test
    @DisplayName("UC6-AC8 passes — and the test gap noticed beside it changes nothing")
    void ac8PassesAndItsEvidenceGapIsKeptBesideIt() {
        // The requirement says the implementation must reject cancellation at the exact start
        // instant. It does. The requirement does not say a test must exist, so a missing test
        // is not silently promoted into a criterion nobody wrote.
        Judgment judgment = judge();

        assertEquals(JudgmentStatus.PASS, judgment.status(), "all six requirements were established");
        assertTrue(judgment.checks().stream()
            .filter(c -> c.name().equals("UC6-AC8")).findFirst().orElseThrow().passed(),
            "UC6-AC8 is satisfied by the implementation");

        Observation gap = Observation.of(judgment).stream()
            .filter(o -> o.requirementId().equals("UC6-AC8")).findFirst().orElseThrow();
        assertTrue(gap.message().toLowerCase().contains("boundary"), gap.message());
        assertTrue(gap.locations().stream().anyMatch(l -> l.contains("AppointmentServiceTests.java")),
            "the observation keeps a location a reader can open: " + gap.locations());
    }

    @Test
    @DisplayName("Observations survive replay and take no part in the verdict")
    void observationsSurviveOfflineReplayAndTakeNoPartInTheRoster() {
        // The recording is the agent's verbatim text, so the whole Judgment -- including its
        // non-binding metadata -- is re-derived on every replay.
        Judgment judgment = judge();

        assertEquals(6, judgment.checks().size(), "six requirements were asked; six answered");
        assertEquals(6, judgment.metadata().get("criteriaTotal"));
        assertFalse(Observation.of(judgment).isEmpty(), "the recording carries observations");
        assertTrue(Observation.of(judgment).size() < judgment.checks().size()
                || judgment.status() == JudgmentStatus.PASS,
            "however many observations there are, the verdict is decided by the checks alone");
        assertNull(judgment.score(), "no score is introduced by any of this");
    }

    private static Judgment judge() {
        List<EarsCriterion> slice = EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE);
        Path workspace = Candidate.workspace();
        return EarsJudge.create("appointment-cancellation", slice, JudgeBackends.forRecording(workspace, "ears-uc6-cancellation"))
            .judge(Candidate.contextFor(workspace));
    }

    @Test
    @DisplayName("The parser is the denominator: six criteria, not five")
    void theSliceIsSixCriteriaFromTheSpecification() {
        // The parser is the denominator. Asking for six and silently getting five would
        // make every downstream count a lie nothing else can detect.
        assertEquals(52, EarsCriterion.from(CRITERIA).size());
        assertEquals(6, EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE).size());
    }
}
