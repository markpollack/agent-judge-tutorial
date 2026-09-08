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
 * Should I merge this? Asked of the complete use case, as a merge gate.
 *
 * <p><b>This is expected to be RED.</b> Fifty-one of fifty-two requirements were established and one
 * — {@code UC6-AC41} — could not be. The gate requires PASS, so it does not go green.
 *
 * <p>That is the whole argument for putting this in JUnit rather than in a report. Nobody has to
 * remember to read it, and nobody has to decide what 51 out of 52 means. The bar was
 * <em>every required criterion affirmatively established</em>, the bar was not met, and the tool
 * every Java developer already trusts says so in the colour it uses for "not yet".
 *
 * <p>Same judge and same document as {@link EarsUseCaseTest}, which asserts the recorded ABSTAIN as
 * a regression contract and is therefore green. Different question, different subject.
 */
@DisplayName("Should I merge? · the complete UC6 specification")
class ShouldIMergeBehaviorDemo {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    private static final Path WORKSPACE = Candidate.workspace();

    /** The repository under evaluation. */
    private static final JudgmentContext context = Candidate.contextFor(WORKSPACE);

    /** Where the judgment comes from — here, a verbatim response captured from a live run. */
    private static final JudgeModel model =
        JudgeBackends.forRecording(WORKSPACE, "spec-conformance-uc6");

    @Test
    @DisplayName("Should I merge? All 52 UC6 requirements must PASS")
    void shouldMergeUc6Behavior() {
        var requirements = EarsCriterion.from(CRITERIA);

        var judge = EarsJudge.create("appointment-lifecycle", requirements, model);

        assertPass(judge, context);
    }
}
