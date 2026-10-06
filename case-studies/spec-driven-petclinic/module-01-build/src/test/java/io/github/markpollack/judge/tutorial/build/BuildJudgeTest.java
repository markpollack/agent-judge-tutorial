package io.github.markpollack.judge.tutorial.build;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.exec.BuildSuccessJudge;
import static io.github.markpollack.judge.assertj.Assertions.assertThat;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import io.github.markpollack.judge.tutorial.support.Candidate;

/**
 * The same judge the demo runs, as a test.
 *
 * <p>This is the ordinary way a team would adopt any of this: the demo is what a stage needs, and
 * the test is what a build needs. Both construct the same judge over the same workspace.
 */
class BuildJudgeTest {

    @Test
    void theCandidateBuildsAndItsTestsPass() {
        Path workspace = Candidate.workspace();
        assertThat(BuildSuccessJudge.maven("test").evidence(workspace).build()).isPassed();
    }
}
