package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.requirements.EarsCriterion;
import io.github.markpollack.judge.ai.requirements.EarsJudge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;

import static io.github.markpollack.judge.junit.JudgeAssertions.assertPass;

/**
 * Should I merge this? Asked of six requirements, as a merge gate.
 *
 * <h2>This is a policy, not a regression test</h2>
 *
 * <p>{@link EarsSliceTest} beside this one asserts that a recorded run still reproduces its known
 * outcome — a regression contract, and green whatever that outcome was. This class asserts something
 * different and much simpler: <b>PASS is required.</b> If the implementation does not establish every
 * requirement, this goes red, which is what a merge gate is for.
 *
 * <p>Both are legitimate and their subjects differ. One tests the evaluator. This one tests the
 * subject.
 *
 * <h2>Named {@code Demo}, deliberately</h2>
 *
 * <p>Surefire discovers {@code *Test}, so a class named {@code *Demo} stays out of the ordinary
 * build while IntelliJ still offers a gutter arrow beside {@code @Test}. That matters because two of
 * this demo's three siblings are <em>supposed</em> to be red, and a build that is intentionally red
 * teaches people to ignore red.
 */
@DisplayName("Should I merge? · six UC6 requirements")
class ShouldIMergeSliceDemo {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    private static final String[] SLICE =
        { "UC6-AC7", "UC6-AC8", "UC6-AC9", "UC6-AC10", "UC6-AC11", "UC6-AC12" };

    private static final Path WORKSPACE = Candidate.workspace();

    /** The repository under evaluation. */
    private static final JudgmentContext context = Candidate.contextFor(WORKSPACE);

    /** Where the judgment comes from — here, a verbatim response captured from a live run. */
    private static final JudgeModel model =
        JudgeBackends.forRecording(WORKSPACE, "ears-uc6-cancellation");

    @Test
    @DisplayName("Should I merge? Six UC6 requirements must PASS")
    void shouldMergeThisSlice() {
        var requirements = EarsCriterion.select(EarsCriterion.from(CRITERIA), SLICE);

        var judge = EarsJudge.create("appointment-cancellation", requirements, model);

        assertPass(judge, context);
    }
}
