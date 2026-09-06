package io.github.markpollack.judge.tutorial.spec;

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
import io.github.markpollack.judge.jury.AllMustPassStrategy;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.architecture.JudgeBackends;
import io.github.markpollack.judge.tutorial.architecture.RecordedJudgeModel;

/**
 * Answer every one of a numbered list of written requirements against a workspace.
 *
 * <p>Two modules use this and they ask different kinds of question. Module 06 asks a use case's
 * 52 acceptance criteria: did the agent build what was asked. Module 07 asks the feature's 13
 * architectural rules: does the code hold to the structure the design laid down. Same instrument,
 * different documents, and the difference in how cleanly it answers them is the finding.
 *
 * <p>The rubric is not invented here. It is the source document's own numbered requirements, read
 * verbatim with their identifiers, so the judge answers the questions that were asked rather than
 * questions it chose. Module 03 had to manufacture its criteria and was unstable until they were
 * fixed; here they arrive fixed.
 *
 * <h2>Three outcomes, and the third one is not a cop-out</h2>
 *
 * <p>A requirement settled by reading a comparison operator is PASS or FAIL. A requirement about
 * runtime behaviour that nothing available exercises is ABSTAIN. Forcing that third kind into
 * PASS or FAIL is guessing, and a judge that guesses on a tenth of a specification has told you
 * nothing about the tenth and damaged what it told you about the rest.
 *
 * <h2>What it does not tell you</h2>
 *
 * <p>That the requirements were right. This answers "was this satisfied", which is a different
 * question from whether asking for it was wise. Different oracles, different modules.
 */
public final class CriteriaAuditJudge {

    private CriteriaAuditJudge() {
    }

    static JudgePromptTemplate templateFor(String name, List<? extends Criterion> criteria) {
        StringBuilder list = new StringBuilder();
        for (Criterion criterion : criteria) {
            list.append("  ").append(criterion.asPrompt()).append('\n');
        }
        return JudgePromptTemplate.fromString(name, """
            You are auditing a Java codebase against written requirements it was built to meet.
            You are in the implementation's root. Read files, grep, and inspect tests.

            Answer every one of the %d acceptance criteria below. Do not add criteria,
            do not merge two into one, and do not skip one because it looks obvious or
            looks hard. The document asked %d questions and owes %d answers.

            For each, reply with exactly one line:

              <criterion-id>: PASS|FAIL|CANNOT_DETERMINE - <one sentence, citing a file>

            PASS            the code demonstrably does this, and you can point at where
            FAIL            the code demonstrably does not
            CANNOT_DETERMINE  this cannot be settled from the code and tests available

            CANNOT_DETERMINE is a real answer and is expected for some of these. Use it
            rather than guessing. A criterion about behaviour under conditions nothing
            here exercises is not a PASS.

            Do not state an overall verdict. You assess each criterion; deciding what the
            set of assessments means is not your job.

            THE REQUIREMENTS

            %s
            """.formatted(criteria.size(), criteria.size(), criteria.size(), list.toString()));
    }

    /** Build the judge over a document's full requirement set, live or replayed. */
    public static ModelBackedJudge create(String name, String description, Path workspace,
            List<? extends Criterion> criteria, String recording) {
        return create(name, description, criteria,
            JudgeBackends.backendFor(workspace, Duration.ofMinutes(20), recording));
    }

    /**
     * The same judge over a supplied backend.
     *
     * <p>This is the seam the tests use. A judge whose answers can only be obtained by paying an
     * agent for twenty minutes is a judge whose verdict logic never gets tested, and the verdict
     * logic is the part that decides what a set of answers means.
     */
    public static ModelBackedJudge create(String name, String description,
            List<? extends Criterion> criteria, JudgeModel model) {
        return ModelBackedJudge.builder()
            .name(name)
            .description(description)
            .promptTemplate(templateFor(name, criteria))
            .model(model)
            .judgmentClassifier(classifier(criteria))
            .build();
    }

    /**
     * Turns per-criterion answers into a judgment that keeps every one of them.
     *
     * <p>Abstentions leave the aggregation population, so the gate is computed over the criteria
     * that could actually be settled. That is correct and it is also dangerous, because a
     * requirement nobody could check stops affecting the verdict. The count is therefore carried
     * in the reasoning and the metadata, so it cannot vanish into a denominator nobody reads.
     */
    private static JudgmentClassifier classifier(List<? extends Criterion> criteria) {
        return response -> {
            String text = response.text() == null ? "" : response.text().strip();

            // A missing recording is the judge's own misconfiguration, not a fact about the
            // subject, and it must not be reported as though the agent had misbehaved. Blaming
            // the subject for the author's mistake is how an unrunnable judge reads as a finding.
            if (RecordedJudgeModel.NO_RECORDING.equals(text)) {
                return Judgment.error("No recording to replay; capture one with "
                    + "AGENT_JUDGE_TUTORIAL_AGENT=live AGENT_JUDGE_TUTORIAL_CAPTURE=<name>");
            }
            Object successful = response.metadata() == null ? null : response.metadata().get("successful");
            if (Boolean.FALSE.equals(successful)) {
                return Judgment.error("The auditing agent did not complete its run");
            }
            if (text.isEmpty()) {
                return Judgment.error("No audit was produced for this candidate");
            }

            Map<String, String> ids = new LinkedHashMap<>();
            criteria.forEach(c -> ids.put(c.id(), c.title()));

            Map<String, JudgmentStatus> outcome = new LinkedHashMap<>();
            Map<String, String> evidence = new LinkedHashMap<>();

            for (String line : text.lines().map(String::strip).toList()) {
                int colon = line.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String id = line.substring(0, colon).strip().replaceAll("[^A-Za-z0-9-]", "");
                if (!ids.containsKey(id) || outcome.containsKey(id)) {
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

            // Roster guard against the source document's own denominator. A criterion the
            // audit skipped is not a criterion that passed, and answering 47 of 52 is
            // not an audit of 52.
            List<String> unanswered = ids.keySet().stream().filter(id -> !outcome.containsKey(id)).toList();
            if (!unanswered.isEmpty()) {
                return Judgment.error("The audit did not answer " + unanswered.size() + " of "
                    + ids.size() + " criteria, beginning with " + unanswered.get(0));
            }

            List<Judgment> perCriterion = new ArrayList<>();
            List<Check> checks = new ArrayList<>();
            List<String> abstained = new ArrayList<>();
            for (String id : ids.keySet()) {
                JudgmentStatus status = outcome.get(id);
                String why = evidence.get(id);
                switch (status) {
                    case PASS -> {
                        perCriterion.add(Judgment.pass(id + ": " + why));
                        checks.add(Check.pass(id, why));
                    }
                    case FAIL -> {
                        perCriterion.add(Judgment.fail(id + ": " + why));
                        checks.add(Check.fail(id, why));
                    }
                    default -> {
                        perCriterion.add(Judgment.abstain(id + ": " + why));
                        abstained.add(id);
                    }
                }
            }

            Judgment rolled = new AllMustPassStrategy().aggregate(perCriterion, Map.of());
            long passed = outcome.values().stream().filter(s -> s == JudgmentStatus.PASS).count();
            long failed = outcome.values().stream().filter(s -> s == JudgmentStatus.FAIL).count();

            String reasoning = "%d of %d criteria pass, %d fail, %d could not be determined"
                .formatted(passed, ids.size(), failed, abstained.size());

            if (rolled.status() != JudgmentStatus.PASS && rolled.status() != JudgmentStatus.FAIL) {
                return Judgment.builder().abstain()
                    .reasoning(reasoning + "; nothing was determinable, so nothing was judged")
                    .metadata("criteriaTotal", ids.size())
                    .metadata("undetermined", String.join(",", abstained))
                    .build();
            }
            return Judgment.verdict(rolled.status() == JudgmentStatus.PASS)
                .reasoning(reasoning)
                .checks(checks)
                .metadata("criteriaTotal", ids.size())
                .metadata("determined", checks.size())
                .metadata("undetermined", String.join(",", abstained))
                .build();
        };
    }
}
