package io.github.markpollack.judge.tutorial.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.ai.JudgmentClassifier;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.jury.AllMustPassStrategy;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

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

    /**
     * The criteria, fixed here rather than invented by the model on each run.
     *
     * <p>This is the whole reliability fix. Left to choose its own criteria, the judge agreed on
     * the facts every run and disagreed about how to carve them: the same missing
     * ClinicServiceTests case became a named criterion that failed in one run and was absorbed
     * into a broader criterion that passed in two others. Verdict followed carving.
     *
     * <p>Fixing the set fixes the denominator. A run that cannot invent a sixth criterion cannot
     * fail on one, and a run that cannot merge two cannot hide one inside the other.
     */
    public static final List<String> CRITERIA = List.of(
        "repository-query", "controller-handler", "search-form", "web-tests", "persistence-tests");

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

        Assess the change against EXACTLY these five criteria and no others. Do not
        add criteria, do not merge two into one, and do not skip one because it seems
        minor. Answer every one.

          repository-query   the new finder is a derived Spring Data query named and
                             documented like the existing findByLastNameStartingWith
          controller-handler the new handler mirrors processFindForm's control flow:
                             null-to-empty, strip, rejectValue with "notFound",
                             single-result redirect, and reuse of addPaginationModel
          search-form        findOwners.html offers a form for the new search, using a
                             message key that already exists
          web-tests          OwnerControllerTests covers the new handler in the style of
                             the existing find-form tests
          persistence-tests  ClinicServiceTests exercises the new finder against the
                             database, as it does for every other repository finder

        Judge conformance to the established pattern, not whether that pattern is good.
        Faithful duplication of an existing shape is conformance. Ignore formatting and
        imports: this project enforces those with spring-javaformat and Checkstyle.

        Do NOT state an overall verdict. You assess each criterion; deciding what the
        set of assessments means is not your job.

        Answer in exactly this form, five lines, nothing else, no markup:

          repository-query: PASS|FAIL - <one sentence of evidence, citing a file>
          controller-handler: PASS|FAIL - <one sentence of evidence, citing a file>
          search-form: PASS|FAIL - <one sentence of evidence, citing a file>
          web-tests: PASS|FAIL - <one sentence of evidence, citing a file>
          persistence-tests: PASS|FAIL - <one sentence of evidence, citing a file>
        """);

    /** Build the judge. The backend is the only thing that differs between live and CI. */
    public static ModelBackedJudge create(Path workspace) {
        return judge("architectural-conformance",
            "Does this change follow the conventions the surrounding code uses?",
            TEMPLATE, workspace, "architectural-conformance-city-search", Duration.ofMinutes(5));
    }

    /**
     * Assemble one stage of this judge.
     *
     * <p>Module 04 uses the same classifier and the same backend selection with a different
     * prompt, because it is the same judge investigating rather than being handed evidence.
     * The concept grows; it does not fork.
     */
    public static ModelBackedJudge judge(String name, String description, JudgePromptTemplate template,
            Path workspace, String recording, Duration timeout) {
        JudgeModel backend = JudgeBackends.live()
            ? JudgeBackends.capturing(JudgeBackends.liveBackend(workspace, timeout), recording)
            : new RecordedJudgeModel(recording);

        return ModelBackedJudge.builder()
            .name(name)
            .description(description)
            .promptTemplate(template)
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

            Map<String, Check> found = new LinkedHashMap<>();
            for (String line : text.lines().map(String::strip).toList()) {
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String name = line.substring(0, colon).strip().replaceAll("</?[^>]+>", "").strip();
                if (!CRITERIA.contains(name)) {
                    continue;
                }
                String rest = line.substring(colon + 1).strip();
                boolean ok = rest.toUpperCase().startsWith("PASS");
                int dash = rest.indexOf('-');
                String detail = dash < 0 ? rest : rest.substring(dash + 1).strip();
                found.put(name, ok ? Check.pass(name, detail) : Check.fail(name, detail));
            }

            // Roster guard. A criterion the model skipped is not a criterion that passed,
            // and four assessments out of five is not an assessment of five. Without this
            // an omission silently shrinks the denominator and improves the result.
            List<String> missing = CRITERIA.stream().filter(c -> !found.containsKey(c)).toList();
            if (!missing.isEmpty()) {
                return Judgment.error("The review did not assess " + missing.size() + " of "
                    + CRITERIA.size() + " criteria: " + String.join(", ", missing));
            }

            // The model assessed; the gate decides. Rolling up in code rather than asking
            // the model for a verdict is what makes the same evidence produce the same
            // answer every run.
            List<Judgment> perCriterion = CRITERIA.stream()
                .map(found::get)
                .map(check -> check.passed()
                    ? Judgment.pass(check.name() + ": " + check.message())
                    : Judgment.fail(check.name() + ": " + check.message()))
                .toList();

            Judgment rolled = new AllMustPassStrategy().aggregate(perCriterion, Map.of());

            // The population is non-empty and every member is PASS or FAIL, so the
            // strategy can only return PASS or FAIL here. If it ever returns something
            // else, hand that back untouched rather than dressing it up as a verdict.
            if (rolled.status() != JudgmentStatus.PASS && rolled.status() != JudgmentStatus.FAIL) {
                return rolled;
            }

            // Keep the parts. The aggregate cannot say which criterion binds, and the
            // binding one is what anybody acts on.
            return Judgment.verdict(rolled.status() == JudgmentStatus.PASS)
                .reasoning(rolled.reasoning())
                .checks(CRITERIA.stream().map(found::get).toList())
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
