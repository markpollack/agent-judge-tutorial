package io.github.markpollack.judge.tutorial.support;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.ai.model.JudgeModelResponse;
import io.github.markpollack.judge.result.Check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a reply from the second tier means.
 *
 * <p>Written from what the tier was asked for, not from what the parser happens to do. The
 * load-bearing cases are the ones where the investigation disagrees with the lead or declines to
 * answer, because a second tier that can only agree is not a second opinion.
 */
class InvestigationTest {

    private static final Rfc2119Constraint RULE_4 = new Rfc2119Constraint("RULE-4", "MUST",
        "acquire locks in the order Owner, Pet, Vet, SchedulingRequest.",
        "Stable row locking serializes races on existing resource rows.");

    private static final Check LEAD = Check.fail("RULE-4",
        "createStaffOffer locks SchedulingRequest at StaffFallbackService.java:247 before Owner at :258");

    @Test
    void awellFormedReplyIsParsed() {
        Investigation found = parse("""
            OUTCOME: ESCALATED
            CONSEQUENCE: Two concurrent operations on one request can wait on each other forever.
            REACHABILITY: REACHABLE
            ARGUMENT: One path takes the request first; the other takes the owner first.
            CITATION: StaffFallbackService.java:248 - locks SchedulingRequest
            CITATION: ReservationService.java:121 - locks Owner first
            """);

        assertEquals(Investigation.Outcome.ESCALATED, found.outcome());
        assertEquals(Investigation.Reachability.REACHABLE, found.reachability());
        assertTrue(found.consequence().startsWith("Two concurrent operations"));
        assertEquals(2, found.citations().size());
        assertEquals(List.of("StaffFallbackService.java:248", "ReservationService.java:121"),
            found.locations());
    }

    /**
     * The exit criterion for this tier: a de-escalation must be representable, and must not be
     * shaped like a failure. An investigation that says the lead was overstated has done its job.
     */
    @Test
    void adeEscalationIsARealResultAndNotAFailure() {
        Investigation found = parse("""
            OUTCOME: DE_ESCALATED
            CONSEQUENCE: The rule is violated but the feared leak cannot occur.
            REACHABILITY: NOT_REACHABLE
            ARGUMENT: The field is never populated; its only writes set null.
            CITATION: SchedulingRequest.java:151 - setFullAiResponse(null)
            """);

        assertEquals(Investigation.Outcome.DE_ESCALATED, found.outcome());
        assertEquals(Investigation.Reachability.NOT_REACHABLE, found.reachability());
        assertTrue(found.changedTheLead(), "a de-escalation changed what the lead claimed");
        assertFalse(found.consequence().isBlank(), "a de-escalation still has a consequence");
    }

    /** NOT_REACHABLE ends in REACHABLE. A prefix test in the wrong order inverts the answer. */
    @Test
    void notReachableIsNotReadAsReachable() {
        assertEquals(Investigation.Reachability.NOT_REACHABLE, parse("""
            OUTCOME: CONFIRMED
            CONSEQUENCE: Something.
            REACHABILITY: NOT_REACHABLE
            """).reachability());
    }

    @Test
    void confirmedAndCannotEstablishDoNotChangeTheLead() {
        assertFalse(parse("""
            OUTCOME: CONFIRMED
            CONSEQUENCE: Exactly as reported.
            REACHABILITY: REACHABLE
            """).changedTheLead());

        assertFalse(parse("""
            OUTCOME: CANNOT_ESTABLISH
            CONSEQUENCE: Could not be settled from the code available.
            REACHABILITY: UNDETERMINED
            """).changedTheLead());
    }

    /** A reachability argument has to name a path, which takes more than one line. */
    @Test
    void amultiLineArgumentIsKept() {
        Investigation found = parse("""
            OUTCOME: ESCALATED
            CONSEQUENCE: Deadlock.
            REACHABILITY: REACHABLE
            ARGUMENT: Path A takes the request, then the owner.
            Path B takes the owner, then the request.
            Both are reachable from a controller.
            """);

        assertEquals(3, found.reachabilityArgument().lines().count());
        assertTrue(found.reachabilityArgument().contains("Path B"));
    }

    /**
     * A reply that did not answer the question is not half an answer. Inventing the missing field
     * would make the tier look like it worked, which is the one outcome worse than saying it did
     * not.
     */
    @Test
    void areplyMissingTheConsequenceIsNotAnInvestigation() {
        assertTrue(Investigation.parse("RULE-4", """
            OUTCOME: ESCALATED
            REACHABILITY: REACHABLE
            ARGUMENT: Something happened.
            """).isEmpty());
    }

    @Test
    void areplyMissingTheReachabilityIsNotAnInvestigation() {
        assertTrue(Investigation.parse("RULE-4", """
            OUTCOME: ESCALATED
            CONSEQUENCE: Something goes wrong.
            """).isEmpty());
    }

    @Test
    void anemptyReplyIsNotAnInvestigation() {
        assertTrue(Investigation.parse("RULE-4", "").isEmpty());
        assertTrue(Investigation.parse("RULE-4", null).isEmpty());
    }

    /** DD-8: a missing recording is the operator's problem, never a finding about the subject. */
    @Test
    void amissingRecordingBlamesTheInstrument() {
        IllegalStateException problem = assertThrows(IllegalStateException.class,
            () -> Investigator.investigate(RULE_4, LEAD,
                request -> new JudgeModelResponse(RecordedJudgeModel.NO_RECORDING, "recorded", null,
                    java.util.Map.of("successful", false))));

        assertTrue(problem.getMessage().contains("capture one with"),
            "the message must tell the operator what to do");
    }

    @Test
    void anincompleteAgentRunBlamesTheInstrument() {
        IllegalStateException problem = assertThrows(IllegalStateException.class,
            () -> Investigator.investigate(RULE_4, LEAD,
                request -> new JudgeModelResponse("partial output", "live", null,
                    java.util.Map.of("successful", false))));

        assertTrue(problem.getMessage().contains("did not complete"));
    }

    /**
     * The framing constraint is the design, so it is asserted rather than trusted. If someone
     * later rewrites this prompt to ask the agent to verify the finding, this fails.
     */
    @Test
    void thepromptAsksForConsequenceAndNeverForVerification() {
        String prompt = Investigator.promptFor(RULE_4, LEAD);

        assertTrue(prompt.contains("Establish the CONSEQUENCE"));
        assertTrue(prompt.contains("REACHABLE"));
        assertTrue(prompt.contains("DE_ESCALATED"),
            "de-escalation must be offered, or the tier can only agree");
        // Line-wrapped in the text block, so match a fragment that cannot straddle the break.
        assertTrue(prompt.contains("you are not being asked"),
            "the prompt must say plainly that verification is not the job");
        assertTrue(prompt.contains("not the same as verifying"));
        assertFalse(prompt.contains("Verify this finding"));
        assertFalse(prompt.contains("Confirm that"));

        assertTrue(prompt.contains("RULE-4"), "the requirement travels into the prompt");
        assertTrue(prompt.contains("Stable row locking"), "so does the author's reason");
        assertTrue(prompt.contains("StaffFallbackService.java:247"), "so does the lead");
    }

    private static Investigation parse(String reply) {
        Optional<Investigation> found = Investigation.parse("RULE-4", reply);
        assertTrue(found.isPresent(), "expected a parseable investigation");
        return found.get();
    }
}
