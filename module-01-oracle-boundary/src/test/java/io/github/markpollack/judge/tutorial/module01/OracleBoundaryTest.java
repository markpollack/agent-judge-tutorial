package io.github.markpollack.judge.tutorial.module01;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import io.github.markpollack.judge.junit.JudgeAssertions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The boundary, as a test class.
 *
 * <p>The first three tests are ordinary JUnit and stay that way: their oracle is known,
 * so {@code assertTrue} already carries it. The rest ask a question no assertion in this
 * file could have expressed, and go through a judge to ask it.
 */
class OracleBoundaryTest {

    private static final Path WORKSPACE = Path.of("test-workspace");

    private static final String REPORT_CONTROLLER = "src/main/java/com/example/ReportController.java";

    private static final String HELLO_CONTROLLER = "src/main/java/com/example/HelloController.java";

    private static final String GOAL = "Expose a sales report endpoint";

    // ------------------------------------------------------------- Known oracle
    // An exact answer exists. Do not reach for a judge.

    @Test
    void theControllerExists() {
        assertTrue(Files.exists(WORKSPACE.resolve(REPORT_CONTROLLER)));
    }

    @Test
    void itDeclaresTheExpectedPackage() throws IOException {
        assertTrue(Files.readString(WORKSPACE.resolve(REPORT_CONTROLLER)).contains("package com.example;"));
    }

    @Test
    void itHasTheRequestedMethod() throws IOException {
        assertTrue(Files.readString(WORKSPACE.resolve(REPORT_CONTROLLER)).contains("String report("));
    }

    // ---------------------------------------------------------- Judgment oracle
    // No expected value exists. The oracle has to be produced.

    @Test
    void reportControllerDoesNotFitTheCodebaseConventions() {
        Judgment judgment = JudgeAssertions.assertFail(judgeFor(REPORT_CONTROLLER));

        // The judgment kept its parts, so the binding criterion is recoverable.
        assertTrue(judgment.checks().stream()
                .anyMatch(check -> check.id().equals("layering")
                    && check.judgment().status() == JudgmentStatus.FAIL),
            "layering should be the criterion that binds");
    }

    /**
     * The negative control. A judge that only ever returns FAIL is a constant, and a
     * suite that never watches it return PASS cannot tell the difference.
     */
    @Test
    void helloControllerDoesFitTheCodebaseConventions() {
        JudgeAssertions.assertPass(judgeFor(HELLO_CONTROLLER));
    }

    /**
     * A stand-in model with nothing recorded for the subject must not approve it. An
     * evaluation that passes because it examined nothing is the failure mode that reads
     * as success.
     */
    @Test
    void anUnreviewedSubjectErrorsRatherThanPassing() {
        var unreviewed = new ArchitecturalFitJudge.Review(
            GOAL, "class SomethingElse {}", "class SomethingElse {}");

        Judgment judgment = JudgeAssertions.assertStatus(
            JudgmentStatus.ERROR, ArchitecturalFitJudge.create(unreviewed));

        assertEquals("No review was produced for this subject", judgment.reasoning());
    }

    private static io.github.markpollack.judge.Judge judgeFor(String subjectPath) {
        return ArchitecturalFitJudge.create(ArchitecturalFitJudge.reviewOf(WORKSPACE, GOAL, subjectPath));
    }
}
