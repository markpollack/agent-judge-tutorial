package io.github.markpollack.judge.tutorial.module01;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.ai.JudgmentClassifier;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.judgment.Check;
import io.github.markpollack.judge.judgment.Judgment;

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
     * The evidence this judge reads: the goal, the controller the codebase already has,
     * and the controller under review. It is bound when the judge is built, so running
     * the judge takes no argument.
     */
    public record Review(String goal, String conventionSource, String subjectSource) {
    }

    /**
     * The prompt is ordinary text rendered from the evidence. Nothing here is Agent Eval
     * magic: {@code {{goal}}}, {@code {{conventionSource}}} and {@code {{subjectSource}}}
     * are substituted from the {@link Review} the caller built.
     */
    private static final JudgePromptTemplate TEMPLATE = JudgePromptTemplate.fromString(
        "architectural-fit",
        """
        You are reviewing a change to an existing Java codebase.

        This is the controller the codebase already has. Every controller here is
        expected to resemble it:

        {{conventionSource}}

        This is the controller the agent added:

        {{subjectSource}}

        The goal the agent was given was: {{goal}}

        Does the added controller fit the architectural conventions and idioms of this
        codebase, without introducing unnecessary complexity?

        Answer with one line per criterion, then a verdict:

          <criterion>: PASS|FAIL - <one sentence>
          VERDICT: PASS|FAIL
          SUMMARY: <one sentence>
        """);

    /** Build the judge for one review. Replace the model to call a real one; nothing else changes. */
    public static ModelBackedJudge<Review> create(Review review) {
        return ModelBackedJudge.<Review>builder()
            .name("architectural-fit")
            .description("Does this controller fit the conventions and idioms of this codebase?")
            .promptTemplate(TEMPLATE)
            .variables(evidence -> Map.of(
                "goal", evidence.goal(),
                "conventionSource", evidence.conventionSource(),
                "subjectSource", evidence.subjectSource()))
            .model(new FixtureEvalModel())
            .judgmentClassifier(classifier())
            .evidence(review)
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
            if (text.isEmpty() || FixtureEvalModel.NO_RECORDING.equals(text)) {
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

    /** Read the evidence for one controller: the codebase's convention and the subject. */
    public static Review reviewOf(Path workspace, String goal, String subjectPath) {
        return new Review(goal, read(workspace.resolve(CONVENTION)), read(workspace.resolve(subjectPath)));
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
