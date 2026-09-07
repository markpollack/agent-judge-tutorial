package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.junit.JudgeAssertions;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.EarsCriterion;
import io.github.markpollack.judge.tutorial.support.EarsJudge;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The same judge the demo runs, as a gate.
 *
 * <p>Replays the committed recording, so it is offline, deterministic and free. What a recorded
 * run verifies is the wiring and the semantics — never that the judge is right.
 */
class EarsSliceTest {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    private static final String[] SLICE =
        { "UC6-AC7", "UC6-AC8", "UC6-AC9", "UC6-AC10", "UC6-AC11", "UC6-AC12" };

    @Test
    void theCancellationSliceIsFullyEstablished() {
        List<EarsCriterion> slice = EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE);
        Path workspace = Candidate.workspace();

        JudgeAssertions.assertStatus(JudgmentStatus.PASS,
            EarsJudge.create("appointment-cancellation", workspace, slice, "ears-uc6-cancellation"),
            Candidate.contextFor(workspace));
    }

    @Test
    void theSliceIsSixCriteriaFromTheSpecification() {
        // The parser is the denominator. Asking for six and silently getting five would
        // make every downstream count a lie nothing else can detect.
        assertEquals(52, EarsCriterion.from(CRITERIA).size());
        assertEquals(6, EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE).size());
    }
}
