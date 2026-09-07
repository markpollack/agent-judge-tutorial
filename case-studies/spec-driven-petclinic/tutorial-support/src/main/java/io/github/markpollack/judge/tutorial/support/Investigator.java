package io.github.markpollack.judge.tutorial.support;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModelRequest;
import io.github.markpollack.judge.ai.model.JudgeModelResponse;
import io.github.markpollack.judge.result.Check;

/**
 * Runs the second tier: takes one failed requirement and establishes what it means.
 *
 * <p>The input is a {@link Check} that a judge already produced, not a string somebody typed here.
 * That matters — the whole claim of this module is that tier one's output is tier two's input, and
 * a hand-copied lead would quietly break the chain it is meant to demonstrate.
 *
 * <p>Same {@link JudgeBackends} path as every other model-backed step in the tutorial, so an
 * investigation replays offline from a committed recording exactly like a judgment does.
 */
public final class Investigator {

    /** Long enough for a real investigation; it reads far more than the cited line. */
    private static final Duration TIMEOUT = Duration.ofMinutes(20);

    private Investigator() {
    }

    /**
     * Establish the consequence of one failed constraint, and whether it is reachable.
     *
     * @return the investigation, or empty when the agent replied without answering the question
     * @throws IllegalStateException when the investigation could not be run at all — a missing
     * recording or an incomplete agent run. That is the operator's problem, and saying so is not
     * the same as saying the subject is fine.
     */
    public static Optional<Investigation> investigate(Rfc2119Constraint constraint, Check lead,
            Path workspace, String recording) {
        JudgeModel model = JudgeBackends.backendFor(workspace, TIMEOUT, recording);
        return investigate(constraint, lead, model);
    }

    /** The same investigation over a supplied backend. The seam the tests use. */
    public static Optional<Investigation> investigate(Rfc2119Constraint constraint, Check lead,
            JudgeModel model) {
        JudgeModelResponse response = model.generate(JudgeModelRequest.user(promptFor(constraint, lead)));
        String text = response.text() == null ? "" : response.text().strip();

        if (RecordedJudgeModel.NO_RECORDING.equals(text)) {
            throw new IllegalStateException("No recording to replay; capture one with "
                + "AGENT_JUDGE_TUTORIAL_AGENT=live AGENT_JUDGE_TUTORIAL_CAPTURE=<name>");
        }
        Object successful = response.metadata() == null ? null : response.metadata().get("successful");
        if (Boolean.FALSE.equals(successful)) {
            throw new IllegalStateException("The investigating agent did not complete its run");
        }
        return Investigation.parse(constraint.id(), text);
    }

    /**
     * The prompt, and the reason this module exists.
     *
     * <p>Read what it asks for: <em>establish the consequence and whether it is reachable</em>. It
     * never says verify, confirm, or prove. An agent asked to verify a finding will verify the
     * finding; the second tier then agrees with the first at considerable expense and adds nothing.
     *
     * <p>So de-escalation is named as valuable in the prompt itself, twice, and the outcome
     * vocabulary is expressed relative to the lead rather than relative to how alarming the result
     * is. The measured behaviour of this framing on this corpus was seven escalations, one
     * de-escalation, and four separate narrowings <em>inside</em> escalations — which is what a
     * second opinion is supposed to look like.
     */
    static String promptFor(Rfc2119Constraint constraint, Check lead) {
        return """
            A requirement in this implementation's own design was assessed and did not hold. You are
            in the implementation's root. Read files, grep, and follow the code.

            THE REQUIREMENT, as its author wrote it before the code existed

              %s
              **%s** %s
              Reason: %s

            WHAT THE ASSESSMENT REPORTED

              %s

            YOUR JOB

            Establish the CONSEQUENCE of this requirement not holding, and whether that consequence
            is REACHABLE in this implementation.

            That is not the same as verifying what the assessment said, and you are not being asked
            to. The assessment gave you a place to start reading. What it concluded may be accurate,
            may be understated, may be overstated, and may point at the wrong line. Read past the
            cited location before you decide.

            A result that says the concern is overstated, or that the feared consequence cannot
            actually be reached, is a valuable answer and is what DE_ESCALATED is for. Do not
            manufacture agreement, and do not manufacture alarm.

            EVIDENCE DISCIPLINE

            Every claim carries a file and a line. Every number comes from a command you actually
            ran, not from reading and estimating. If you cannot support a claim, drop it or say the
            question could not be settled.

            REPLY IN EXACTLY THIS FORM

              OUTCOME: CONFIRMED|ESCALATED|DE_ESCALATED|CANNOT_ESTABLISH
              CONSEQUENCE: <one sentence: what goes wrong if this is left alone>
              REACHABILITY: REACHABLE|NOT_REACHABLE|UNDETERMINED
              ARGUMENT: <why that reachability holds; name the paths, do not assert them>
              CITATION: <file:line> - <what is there>
              CITATION: <file:line> - <what is there>

            OUTCOME is relative to the assessment you were given:

              CONFIRMED         accurate as stated
              ESCALATED         worse, wider, or somewhere other than the cited line
              DE_ESCALATED      overstated
              CANNOT_ESTABLISH  neither consequence nor reachability could be settled

            ARGUMENT may run to several lines. If the consequence requires particular conditions to
            be reachable, say which — a narrowed reachability that is precise is worth more than a
            broad one that is vague.
            """.formatted(constraint.id(), constraint.keyword(), constraint.requirement(),
                constraint.reason(), lead.message() == null ? "(no detail)" : lead.message().strip());
    }
}
