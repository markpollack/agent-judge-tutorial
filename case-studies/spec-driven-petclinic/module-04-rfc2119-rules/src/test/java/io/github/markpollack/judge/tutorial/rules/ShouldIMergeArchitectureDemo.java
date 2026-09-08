package io.github.markpollack.judge.tutorial.rules;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.requirements.Rfc2119Constraint;
import io.github.markpollack.judge.ai.requirements.Rfc2119Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;

import static io.github.markpollack.judge.junit.JudgeAssertions.assertPass;

/**
 * Should I merge this? Asked of the architecture, as a merge gate.
 *
 * <p><b>This is expected to be RED</b>, and it is the same three lines as the behavioural gate with
 * one word changed — the document. The build is green, its 290 tests pass, the behavioural
 * specification mostly holds, and eight of thirteen feature-wide architectural MUSTs do not.
 *
 * <p>The failure message names them, with the file and line each was found at, because
 * {@code JudgeAssertions} keeps the evidence rather than reducing it to a boolean. That is the
 * difference between a gate you can act on and a gate that tells you something went wrong.
 */
@DisplayName("Should I merge? · the 13 feature-wide architectural MUSTs")
class ShouldIMergeArchitectureDemo {

    private static final Path RULES = Candidate.SPEC.resolve("rules.md");

    private static final Path WORKSPACE = Candidate.workspace();

    /** The repository under evaluation. */
    private static final JudgmentContext context = Candidate.contextFor(WORKSPACE);

    /** Where the judgment comes from — here, a verbatim response captured from a live run. */
    private static final JudgeModel model =
        JudgeBackends.forRecording(WORKSPACE, "architecture-rules");

    @Test
    @DisplayName("Should I merge? All 13 architectural MUSTs must PASS")
    void shouldMergeArchitecture() {
        var requirements = Rfc2119Constraint.from(RULES);

        var judge = Rfc2119Judge.create("architecture", requirements, model);

        assertPass(judge, context);
    }
}
