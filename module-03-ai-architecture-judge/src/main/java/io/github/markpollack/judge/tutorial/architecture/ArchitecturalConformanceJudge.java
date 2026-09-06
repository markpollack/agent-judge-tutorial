package io.github.markpollack.judge.tutorial.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.github.markpollack.judge.ai.JudgmentClassifier;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;

/**
 * One question, and it is not a matter of taste:
 *
 * <blockquote>Does this change follow the conventions the surrounding code already uses, without
 * adding complexity it does not need?</blockquote>
 *
 * <p>There is no expected value to compare against. "Follows the conventions of this codebase" is
 * not a string, a count, or an exit code, so the oracle has to be produced rather than looked up.
 * That, and only that, is what justifies a model here.
 *
 * <p>Some of the evidence it reports is mechanisable, and where it is, a deterministic instrument
 * should own it. This codebase already machine-checks formatting through spring-javaformat and
 * catches unused imports through Checkstyle. What is left over is the part worth asking about.
 *
 * <p>This module supplies a curated evidence set: the diff and the code it should resemble.
 * Module 04 stops supplying evidence and lets the judge go and find it.
 */
public final class ArchitecturalConformanceJudge {

    private ArchitecturalConformanceJudge() {
    }

    private static final JudgePromptTemplate TEMPLATE = JudgePromptTemplate.fromString(
        "architectural-conformance",
        """
        You are reviewing one change to an existing Java codebase.

        The agent was given this task:
        {{goal}}

        This is the code the change should resemble. It is the existing owner search,
        and it is the convention:

        ----- OwnerController.java (existing) -----
        {{metadata.conventionController}}

        ----- OwnerRepository.java (existing) -----
        {{metadata.conventionRepository}}

        This is the change the agent made:

        ----- diff -----
        {{metadata.diff}}

        Question, and only this question: does the change follow the conventions the
        surrounding owner-search code already uses?

        Judge conformance to the existing pattern. Do NOT judge whether that pattern is
        itself good, and do not judge whether mirroring it is the best possible design.
        Duplication that faithfully follows the established shape is conformance, not a
        violation. Whether this codebase should factor its two search flows together is
        a real question, and it is a different one.

        Judge only what the diff shows. Do not report formatting or import hygiene:
        this project already enforces those with spring-javaformat and Checkstyle, and
        a build failure would have caught them before you were asked.

        Answer in exactly this form and nothing else:

          criterion-name: PASS|FAIL - <one sentence of evidence>
          criterion-name: PASS|FAIL - <one sentence of evidence>
          VERDICT: PASS|FAIL
          SUMMARY: <one sentence>

        Use plain criterion names. Do not wrap them in tags or markup.
        """);

    /** Build the judge. The backend is the only thing that differs between live and CI. */
    public static ModelBackedJudge create(Path workspace) {
        String recording = "architectural-conformance-city-search";
        JudgeModel backend = JudgeBackends.live()
            ? JudgeBackends.capturing(JudgeBackends.liveBackend(workspace, Duration.ofMinutes(5)), recording)
            : new RecordedJudgeModel(recording);

        return ModelBackedJudge.builder()
            .name("architectural-conformance")
            .description("Does this change follow the conventions the surrounding code uses?")
            .promptTemplate(TEMPLATE)
            .model(backend)
            .judgmentClassifier(classifier())
            .build();
    }

    /**
     * Turns the review into a judgment that keeps its parts.
     *
     * <p>Every criterion the reviewer named becomes a {@link Check}. Nothing is averaged: four
     * criteria passing and one failing do not become 0.8, because a number cannot say which one
     * binds, and the binding one is the only part anybody acts on.
     */
    private static JudgmentClassifier classifier() {
        return response -> {
            // A failed agent run is ERROR. Parsing its text into a verdict would let an
            // infrastructure failure masquerade as a finding about the code.
            Object successful = response.metadata() == null ? null : response.metadata().get("successful");
            if (Boolean.FALSE.equals(successful)) {
                return Judgment.error("The judging agent did not complete its run");
            }

            String text = response.text() == null ? "" : response.text().strip();
            if (text.isEmpty() || RecordedJudgeModel.NO_RECORDING.equals(text)) {
                return Judgment.error("No review was produced for this subject");
            }

            List<Check> checks = new ArrayList<>();
            Boolean passed = null;
            String summary = null;

            for (String line : text.lines().map(String::strip).toList()) {
                if (line.startsWith("VERDICT:")) {
                    passed = line.substring("VERDICT:".length()).strip().toUpperCase().startsWith("PASS");
                }
                else if (line.startsWith("SUMMARY:")) {
                    summary = line.substring("SUMMARY:".length()).strip();
                }
                else if (line.contains(":") && (line.contains("PASS") || line.contains("FAIL"))) {
                    String name = line.substring(0, line.indexOf(':')).strip()
                        .replaceAll("</?[^>]+>", "")   // models sometimes wrap names in tags
                        .replaceAll("^[-*`\\s]+|[`\\s]+$", "")
                        .strip();
                    String rest = line.substring(line.indexOf(':') + 1).strip();
                    boolean ok = rest.toUpperCase().startsWith("PASS");
                    int dash = rest.indexOf('-');
                    String detail = dash < 0 ? rest : rest.substring(dash + 1).strip();
                    checks.add(ok ? Check.pass(name, detail) : Check.fail(name, detail));
                }
            }

            if (passed == null) {
                return Judgment.error("The review did not state a VERDICT line");
            }
            if (checks.isEmpty()) {
                // A verdict with no criteria behind it is an opinion, not evidence.
                return Judgment.error("The review stated a verdict but named no criteria");
            }
            return Judgment.verdict(passed)
                .reasoning(summary != null ? summary : "No summary given")
                .checks(checks)
                .build();
        };
    }

    /** The curated evidence this module supplies. Module 04 stops supplying it. */
    public static JudgmentContext contextFor(Path workspace, Path baseline, Path patch) {
        String owner = "src/main/java/org/springframework/samples/petclinic/owner/";
        return io.github.markpollack.judge.tutorial.build.PetClinic.contextBuilder(workspace)
            .metadata("conventionController", read(baseline.resolve(owner + "OwnerController.java")))
            .metadata("conventionRepository", read(baseline.resolve(owner + "OwnerRepository.java")))
            .metadata("diff", read(patch))
            .build();
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
