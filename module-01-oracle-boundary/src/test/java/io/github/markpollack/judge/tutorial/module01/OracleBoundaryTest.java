package io.github.markpollack.judge.tutorial.module01;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.junit.JudgeAssertions;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

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
        Judgment judgment = JudgeAssertions.assertFail(
            ArchitecturalFitJudge.create(), contextFor(REPORT_CONTROLLER));

        // The judgment kept its parts, so the binding criterion is recoverable.
        assertTrue(judgment.checks().stream()
                .anyMatch(check -> check.name().equals("layering") && !check.passed()),
            "layering should be the criterion that binds");
    }

    /**
     * The negative control. A judge that only ever returns FAIL is a constant, and a
     * suite that never watches it return PASS cannot tell the difference.
     */
    @Test
    void helloControllerDoesFitTheCodebaseConventions() {
        JudgeAssertions.assertPass(ArchitecturalFitJudge.create(), contextFor(HELLO_CONTROLLER));
    }

    /**
     * A stand-in model with nothing recorded for the subject must not approve it. An
     * evaluation that passes because it examined nothing is the failure mode that reads
     * as success.
     */
    @Test
    void anUnreviewedSubjectErrorsRatherThanPassing() {
        JudgmentContext unreviewed = JudgmentContext.builder()
            .goal(GOAL)
            .workspace(WORKSPACE)
            .status(io.github.markpollack.judge.context.ExecutionStatus.SUCCESS)
            .startedAt(java.time.Instant.now())
            .executionTime(java.time.Duration.ofSeconds(1))
            .metadata("conventionSource", "class SomethingElse {}")
            .metadata("subjectSource", "class SomethingElse {}")
            .build();

        Judgment judgment = JudgeAssertions.assertStatus(
            JudgmentStatus.ERROR, ArchitecturalFitJudge.create(), unreviewed);

        assertEquals("No review was produced for this subject", judgment.reasoning());
    }

    private static JudgmentContext contextFor(String subjectPath) {
        return ArchitecturalFitJudge.contextFor(WORKSPACE, GOAL, subjectPath);
    }
}
