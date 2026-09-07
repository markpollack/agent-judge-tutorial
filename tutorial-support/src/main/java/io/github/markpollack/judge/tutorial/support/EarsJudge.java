package io.github.markpollack.judge.tutorial.support;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.ai.JudgmentClassifier;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

/**
 * Answers every acceptance criterion in a specification against an implementation.
 *
 * <h2>PASS means every requirement was affirmatively established</h2>
 *
 * <p>The rollup is deliberately strict:
 *
 * <pre>
 *   any ERROR        -&gt; ERROR
 *   else any FAIL    -&gt; FAIL
 *   else any ABSTAIN -&gt; ABSTAIN
 *   else             -&gt; PASS
 * </pre>
 *
 * <p>An abstention is not dropped. A written acceptance criterion is required by construction —
 * the specification says it applies — so "could not determine" is not "does not apply". Absorbing
 * it into a passing population would report that the implementation satisfies the complete
 * specification when one requirement was never settled.
 *
 * <h2>The model assesses; Java decides</h2>
 *
 * <p>The model returns one status and one line of evidence per criterion. It does not choose the
 * criteria, count them, decide how many there are, or compute the verdict. Those are this class's
 * job, in Java, where they are inspectable and cannot drift.
 *
 * <p>There is no score. Every criterion stays individually visible, and a failure names the
 * criterion that bound the verdict and where.
 */
public final class EarsJudge {

    private EarsJudge() {
    }

    /** Build the judge over a criteria set, live or replayed. */
    public static ModelBackedJudge create(String name, Path workspace,
            List<EarsCriterion> criteria, String recording) {
        return create(name, criteria, JudgeBackends.backendFor(workspace, Duration.ofMinutes(20), recording));
    }

    /** The same judge over a supplied backend. The seam the tests use. */
    public static ModelBackedJudge create(String name, List<EarsCriterion> criteria, JudgeModel model) {
        return ModelBackedJudge.builder()
            .name(name)
            .description("Did the implementation satisfy the acceptance criteria it was built from?")
            .promptTemplate(templateFor(name, criteria))
            .model(model)
            .judgmentClassifier(classifier(criteria))
            .build();
    }

    static JudgePromptTemplate templateFor(String name, List<EarsCriterion> criteria) {
        StringBuilder list = new StringBuilder();
        criteria.forEach(c -> list.append("  ").append(c.asPrompt()).append('\n'));
        int n = criteria.size();
        return JudgePromptTemplate.fromString(name, """
            You are auditing a Java implementation against the acceptance criteria it was
            built to satisfy. You are in the implementation's root. Read files, grep, and
            inspect tests.

            Answer every one of the %d criteria below. Do not add criteria, do not merge two
            into one, and do not skip one because it looks obvious or looks hard. The
            specification asked %d questions and owes %d answers.

            For each, reply with exactly one line:

              <criterion-id>: PASS|FAIL|CANNOT_DETERMINE - <one sentence, citing a file>

            PASS              the code demonstrably does this, and you can point at where
            FAIL              the code demonstrably does not
            CANNOT_DETERMINE  this cannot be settled from the code and tests available

            Cite a file and line for every claim. If you state a count, obtain it with a
            command rather than by reading and estimating.

            CANNOT_DETERMINE is a real answer. Use it rather than guessing.

            Do not state an overall verdict. You assess each criterion; deciding what the set
            of assessments means is not your job.

            THE CRITERIA

            %s
            """.formatted(n, n, n, list.toString()));
    }

    private static JudgmentClassifier classifier(List<EarsCriterion> criteria) {
        return response -> {
            String text = response.text() == null ? "" : response.text().strip();

            if (RecordedJudgeModel.NO_RECORDING.equals(text)) {
                return Judgment.error("No recording to replay; capture one with "
                    + "AGENT_JUDGE_TUTORIAL_AGENT=live AGENT_JUDGE_TUTORIAL_CAPTURE=<name>");
            }
            Object successful = response.metadata() == null ? null : response.metadata().get("successful");
            if (Boolean.FALSE.equals(successful)) {
                return Judgment.error("The auditing agent did not complete its run");
            }
            if (text.isEmpty()) {
                return Judgment.error("No audit was produced for this implementation");
            }

            Map<String, String> roster = new LinkedHashMap<>();
            criteria.forEach(c -> roster.put(c.id(), c.title()));

            Map<String, JudgmentStatus> outcome = new LinkedHashMap<>();
            Map<String, String> evidence = new LinkedHashMap<>();
            parse(text, roster.keySet(), outcome, evidence);

            // A criterion the audit skipped is not a criterion that passed.
            List<String> unanswered = roster.keySet().stream().filter(id -> !outcome.containsKey(id)).toList();
            if (!unanswered.isEmpty()) {
                return Judgment.error("The audit did not answer " + unanswered.size() + " of "
                    + roster.size() + " criteria, beginning with " + unanswered.get(0));
            }

            List<Check> checks = new ArrayList<>();
            List<String> abstained = new ArrayList<>();
            long passed = 0;
            long failed = 0;
            for (String id : roster.keySet()) {
                JudgmentStatus status = outcome.get(id);
                String why = evidence.get(id);
                switch (status) {
                    case PASS -> {
                        passed++;
                        checks.add(Check.pass(id, why));
                    }
                    case FAIL -> {
                        failed++;
                        checks.add(Check.fail(id, why));
                    }
                    default -> {
                        abstained.add(id);
                        checks.add(Check.fail(id, "could not be established: " + why));
                    }
                }
            }

            // PASS means every required criterion was affirmatively established.
            JudgmentStatus verdict = failed > 0 ? JudgmentStatus.FAIL
                : !abstained.isEmpty() ? JudgmentStatus.ABSTAIN
                : JudgmentStatus.PASS;

            String reasoning = summarize(passed, failed, abstained, roster.size());

            String unestablished = String.join(",", abstained);
            return switch (verdict) {
                case PASS -> Judgment.builder().pass().reasoning(reasoning).checks(checks)
                    .metadata("criteriaTotal", roster.size()).metadata("established", passed)
                    .metadata("unestablished", unestablished).build();
                case FAIL -> Judgment.builder().fail().reasoning(reasoning).checks(checks)
                    .metadata("criteriaTotal", roster.size()).metadata("established", passed)
                    .metadata("unestablished", unestablished).build();
                default -> Judgment.builder().abstain().reasoning(reasoning).checks(checks)
                    .metadata("criteriaTotal", roster.size()).metadata("established", passed)
                    .metadata("unestablished", unestablished).build();
            };
        };
    }

    private static void parse(String text, Iterable<String> ids,
            Map<String, JudgmentStatus> outcome, Map<String, String> evidence) {
        for (String line : text.lines().map(String::strip).toList()) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String id = line.substring(0, colon).strip().replaceAll("[^A-Za-z0-9-]", "");
            boolean known = false;
            for (String candidate : ids) {
                if (candidate.equals(id)) {
                    known = true;
                    break;
                }
            }
            if (!known || outcome.containsKey(id)) {
                continue;
            }
            String rest = line.substring(colon + 1).strip();
            String upper = rest.toUpperCase();
            JudgmentStatus status = upper.startsWith("PASS") ? JudgmentStatus.PASS
                : upper.startsWith("FAIL") ? JudgmentStatus.FAIL
                : upper.startsWith("CANNOT") ? JudgmentStatus.ABSTAIN : null;
            if (status == null) {
                continue;
            }
            int dash = rest.indexOf('-');
            outcome.put(id, status);
            evidence.put(id, dash < 0 ? rest : rest.substring(dash + 1).strip());
        }
    }

    private static String summarize(long passed, long failed, List<String> abstained, int total) {
        if (failed == 0 && abstained.isEmpty()) {
            return "all %d requirements established".formatted(total);
        }
        StringBuilder text = new StringBuilder("%d of %d established".formatted(passed, total));
        if (failed > 0) {
            text.append(", %d not satisfied".formatted(failed));
        }
        if (!abstained.isEmpty()) {
            text.append(", %d could not be established: %s".formatted(abstained.size(), String.join(", ", abstained)));
        }
        return text.toString();
    }
}
