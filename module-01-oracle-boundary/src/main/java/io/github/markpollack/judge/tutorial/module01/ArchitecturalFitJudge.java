package io.github.markpollack.judge.tutorial.module01;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.markpollack.judge.ai.JudgmentClassifier;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;

/**
 * One question:
 *
 * <blockquote>Does this controller fit the architectural conventions and idioms of this
 * codebase, without introducing unnecessary complexity?</blockquote>
 *
 * <p>There is no expected value to compare against. "Fits the conventions of this
 * codebase" is not a string, a count, or an exit code, and "unnecessary complexity" is
 * not greppable. The oracle has to be produced rather than looked up, which is the only
 * thing that justifies a model here.
 *
 * <p>Some of the evidence it reports <em>is</em> mechanisable - concatenating request
 * parameters into SQL is a pattern a scanner should own, and module 05 shows that shape.
 * The verdict is the part that is not. Use the least interpretive instrument that can
 * reliably answer the question, and this is the question that is left over.
 */
public final class ArchitecturalFitJudge {

    private static final String CONVENTION = "src/main/java/com/example/HelloController.java";

    private ArchitecturalFitJudge() {
    }

    /**
     * The prompt is ordinary text rendered from the context. Nothing here is Agent Judge
     * magic: {@code {{goal}}} and {@code {{metadata.*}}} are substituted from the
     * {@link JudgmentContext} the caller built.
     */
    private static final JudgePromptTemplate TEMPLATE = JudgePromptTemplate.fromString(
        "architectural-fit",
        """
        You are reviewing a change to an existing Java codebase.

        This is the controller the codebase already has. Every controller here is
        expected to resemble it:

        {{metadata.conventionSource}}

        This is the controller the agent added:

        {{metadata.subjectSource}}

        The goal the agent was given was: {{goal}}

        Does the added controller fit the architectural conventions and idioms of this
        codebase, without introducing unnecessary complexity?

        Answer with one line per criterion, then a verdict:

          <criterion>: PASS|FAIL - <one sentence>
          VERDICT: PASS|FAIL
          SUMMARY: <one sentence>
        """);

    /** Build the judge. Replace the model to call a real one; nothing else changes. */
    public static ModelBackedJudge create() {
        return ModelBackedJudge.builder()
            .name("architectural-fit")
            .description("Does this controller fit the conventions and idioms of this codebase?")
            .promptTemplate(TEMPLATE)
            .model(new FixtureJudgeModel())
            .judgmentClassifier(classifier())
            .build();
    }

    /**
     * Turns the review text into a judgment that keeps its parts.
     *
     * <p>Every criterion the reviewer named becomes a {@link Check}. The summary becomes
     * the reasoning. Nothing is averaged: five failing criteria and one passing one do
     * not become 0.17, because a number cannot tell you which criterion binds, and that
     * is the only part anybody acts on.
     */
    private static JudgmentClassifier classifier() {
        return response -> {
            String text = response.text().strip();
            if (text.isEmpty() || FixtureJudgeModel.NO_RECORDING.equals(text)) {
                // The judge did not reach a finding. That is ERROR, not FAIL: the
                // subject has not been rejected, it has not been reviewed.
                return Judgment.error("No review was produced for this subject");
            }

            List<Check> checks = new ArrayList<>();
            Boolean passed = null;
            String summary = null;

            for (String line : text.lines().map(String::strip).toList()) {
                if (line.startsWith("VERDICT:")) {
                    passed = line.substring("VERDICT:".length()).strip().equalsIgnoreCase("PASS");
                }
                else if (line.startsWith("SUMMARY:")) {
                    summary = line.substring("SUMMARY:".length()).strip();
                }
                else if (line.contains(":")) {
                    String name = line.substring(0, line.indexOf(':')).strip();
                    String rest = line.substring(line.indexOf(':') + 1).strip();
                    boolean criterionPassed = rest.startsWith("PASS");
                    String detail = rest.contains("-") ? rest.substring(rest.indexOf('-') + 1).strip() : rest;
                    checks.add(criterionPassed ? Check.pass(name, detail) : Check.fail(name, detail));
                }
            }

            if (passed == null) {
                return Judgment.error("Review did not state a VERDICT line");
            }
            return Judgment.verdict(passed)
                .reasoning(summary != null ? summary : "No summary given")
                .checks(checks)
                .build();
        };
    }

    /**
     * Build a context for one controller. The convention and the subject travel as
     * context metadata because that is what the prompt renders from.
     */
    public static JudgmentContext contextFor(Path workspace, String goal, String subjectPath) {
        return contextBuilder(workspace, goal, subjectPath).build();
    }

    /**
     * The same context, still open, for callers that need to add their own metadata -
     * module 06 supplies a coverage baseline alongside these sources.
     */
    public static JudgmentContext.Builder contextBuilder(Path workspace, String goal, String subjectPath) {
        return JudgmentContext.builder()
            .goal(goal)
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(java.time.Instant.now())
            .executionTime(java.time.Duration.ofMinutes(2))
            .metadata("conventionSource", read(workspace.resolve(CONVENTION)))
            .metadata("subjectSource", read(workspace.resolve(subjectPath)));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
