package io.github.markpollack.judge.junit;

import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bridge is an assertion, so the interesting cases are the ones where it must throw.
 * Each test below states the outcome first and derives the expectation from it, rather
 * than from what the code currently does.
 */
class JudgeAssertionsTest {

    private static final JudgmentContext CONTEXT = JudgmentContext.builder()
        .goal("Add a sales report endpoint")
        .workspace(Path.of("test-workspace"))
        .status(ExecutionStatus.SUCCESS)
        .startedAt(Instant.now())
        .executionTime(Duration.ofSeconds(1))
        .build();

    @Test
    void passingJudgmentDoesNotThrow() {
        assertDoesNotThrow(() -> JudgeAssertions.assertPass(Judgment.pass("controller exists")));
    }

    @Test
    void failingJudgmentReportsReasoningAndFailedChecks() {
        Judgment judgment = Judgment.verdict(false)
            .reasoning("ReportController does not follow the codebase's conventions")
            .check(Check.pass("naming", "class name ends in Controller"))
            .check(Check.fail("layering", "opens its own java.sql.Connection"))
            .check(Check.fail("serialization", "builds JSON by string concatenation"))
            .build();

        AssertionFailedError error =
            assertThrows(AssertionFailedError.class, () -> JudgeAssertions.assertPass(judgment));

        assertAll(
            () -> assertTrue(error.getMessage().contains("Expected judgment PASS but was FAIL"),
                error.getMessage()),
            () -> assertTrue(error.getMessage().contains("does not follow the codebase's conventions"),
                error.getMessage()),
            () -> assertTrue(error.getMessage().contains("2 of 3 checks failed"), error.getMessage()),
            () -> assertTrue(error.getMessage().contains("opens its own java.sql.Connection"),
                error.getMessage()),
            // the passing check is evidence, not a failure, and must not be listed as one
            () -> assertTrue(!error.getMessage().contains("class name ends in Controller"),
                error.getMessage()));
    }

    @Test
    void erroredJudgmentIsNotAFail() {
        // A judge that could not complete has rejected nothing. If assertFail accepted
        // ERROR, a broken judge would read in the report as a caught defect.
        AssertionFailedError error = assertThrows(AssertionFailedError.class,
            () -> JudgeAssertions.assertFail(Judgment.error("no JaCoCo report in workspace")));

        assertAll(
            () -> assertTrue(error.getMessage().contains("Expected judgment FAIL but was ERROR"),
                error.getMessage()),
            () -> assertTrue(error.getMessage().contains("did not complete"), error.getMessage()));
    }

    @Test
    void abstainingJudgmentIsNotAFail() {
        AssertionFailedError error = assertThrows(AssertionFailedError.class,
            () -> JudgeAssertions.assertFail(Judgment.abstain("no baseline coverage supplied")));

        assertAll(
            () -> assertTrue(error.getMessage().contains("Expected judgment FAIL but was ABSTAIN"),
                error.getMessage()),
            () -> assertTrue(error.getMessage().contains("cast no vote"), error.getMessage()));
    }

    @Test
    void abstainingJudgmentIsNotAPassEither() {
        assertThrows(AssertionFailedError.class,
            () -> JudgeAssertions.assertPass(Judgment.abstain("not applicable")));
    }

    @Test
    void expectedAndActualAreCarriedForTheIdeDiff() {
        AssertionFailedError error =
            assertThrows(AssertionFailedError.class, () -> JudgeAssertions.assertPass(Judgment.fail("no")));

        assertAll(
            () -> assertEquals(JudgmentStatus.PASS, error.getExpected().getValue()),
            () -> assertEquals(JudgmentStatus.FAIL, error.getActual().getValue()));
    }

    @Test
    void judgeOverloadRunsTheJudgeAndReturnsTheJudgment() {
        Judgment judgment = JudgeAssertions.assertPass(
            Judges.named(context -> Judgment.pass("ran"), "runs"), CONTEXT);

        assertEquals("ran", judgment.reasoning());
    }

    @Test
    void failingVerdictNamesEveryMemberAndExplainsOnlyTheFailingOnes() {
        // Two of three fail, so majority voting reaches FAIL. Note what that implies
        // in the other direction: had only coverage failed, this same jury would have
        // returned PASS at 2 of 3. That is why module 06 does not build a definition
        // of done out of a jury.
        SimpleJury jury = SimpleJury.builder()
            .judge(Judges.named(context -> Judgment.pass("4 tests, 0 failures"), "build"), 1.0)
            .judge(Judges.named(context -> Judgment.verdict(false)
                .reasoning("line coverage 36.0% is below the 90.0% floor")
                .check(Check.fail("line_coverage", "36.0% < 90.0%"))
                .build(), "coverage"), 1.0)
            .judge(Judges.named(context -> Judgment.fail("opens its own java.sql.Connection"),
                "architectural-fit"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        AssertionFailedError error =
            assertThrows(AssertionFailedError.class, () -> JudgeAssertions.assertPass(jury, CONTEXT));

        assertAll(
            () -> assertTrue(error.getMessage().contains("3 judges:"), error.getMessage()),
            () -> assertTrue(error.getMessage().contains("build"), error.getMessage()),
            () -> assertTrue(error.getMessage().contains("architectural-fit"), error.getMessage()),
            () -> assertTrue(error.getMessage().contains("coverage"), error.getMessage()),
            () -> assertTrue(error.getMessage().contains("below the 90.0% floor"), error.getMessage()),
            // a passing member contributes its name, never its reasoning
            () -> assertTrue(!error.getMessage().contains("4 tests, 0 failures"), error.getMessage()));
    }

    @Test
    void passingVerdictDoesNotThrow() {
        SimpleJury jury = SimpleJury.builder()
            .judge(Judges.named(context -> Judgment.pass("compiles"), "build"), 1.0)
            .judge(Judges.named(context -> Judgment.pass("tests green"), "tests"), 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .build();

        assertDoesNotThrow(() -> JudgeAssertions.assertPass(jury, CONTEXT));
    }
}
