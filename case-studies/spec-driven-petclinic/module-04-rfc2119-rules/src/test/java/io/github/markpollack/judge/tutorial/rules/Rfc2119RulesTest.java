package io.github.markpollack.judge.tutorial.rules;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.junit.JudgeAssertions;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.Rfc2119Constraint;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.Rfc2119Judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The architectural design, asserted as a regression contract.
 *
 * <p><b>Not a merge gate.</b> This asserts the recorded FAIL and is therefore green: it verifies the
 * evaluator still reproduces what it produced when captured. {@code ShouldIMergeArchitectureDemo}
 * requires PASS and is red.
 *
 * <p>Same subject as modules 02 and 03. The only things that changed are the authoritative
 * document and the judge that reads it.
 */
@DisplayName("Recorded outcome · the 13 feature-wide architectural MUSTs")
class Rfc2119RulesTest {

    private static final Path RULES = Candidate.SPEC.resolve("rules.md");

    @Test
    @DisplayName("Replay contract · 13 architectural MUSTs → FAIL: 8 violated")
    void theArchitecturalDesignIsViolated() {
        Path workspace = Candidate.workspace();
        JudgeAssertions.assertStatus(JudgmentStatus.FAIL,
            Rfc2119Judge.create("architectural-constraints", Rfc2119Constraint.from(RULES), JudgeBackends.forRecording(workspace, "architecture-rules")),
            Candidate.contextFor(workspace));
    }

    @Test
    @DisplayName("13 checks · 5 hold · 8 violated · 0 undetermined")
    void theRosterIsCompleteAndTheSplitIsNotAccidental() {
        Judgment judgment = judge();

        assertEquals(13, Rfc2119Constraint.from(RULES).size(), "the parser is the denominator");
        assertEquals(13, judgment.checks().size(), "one check per constraint, all retained");
        assertEquals(13, judgment.metadata().get("constraintsTotal"));

        long held = judgment.checks().stream().filter(Check::passed).count();
        assertEquals(5L, held, "5 hold");
        assertEquals(8L, judgment.checks().size() - held, "8 violated");
        assertEquals("", judgment.metadata().get("unestablished").toString(),
            "nothing was left undetermined: every constraint was settled either way");
    }

    @Test
    @DisplayName("The violations are the ones the document names: RULE-1, 2, 4, 5, 8, 10, 11, 12")
    void theViolationsAreTheOnesTheDocumentNames() {
        List<String> violated = judge().checks().stream()
            .filter(check -> !check.passed()).map(Check::name).toList();

        assertEquals(List.of("RULE-1", "RULE-2", "RULE-4", "RULE-5",
            "RULE-8", "RULE-10", "RULE-11", "RULE-12"), violated,
            "reported in the document's order, none dropped or invented");
    }

    @Test
    @DisplayName("Every violation carries a file and line you can open")
    void everyFailureCarriesAnAddress() {
        // A FAIL is only worth what a reader can open. Module 05 turns these into consequences;
        // this module owes them a location and nothing more.
        judge().checks().stream().filter(check -> !check.passed()).forEach(check ->
            assertTrue(check.message().matches("(?s).*[A-Za-z0-9_-]+\\.(java|xml|sql|yml|properties).*"),
                check.name() + " cites no file: " + check.message()));
    }

    @Test
    @DisplayName("All 13 are MUST — and nothing anywhere is scored")
    void allThirteenAreMustAndNothingIsScored() {
        assertTrue(Rfc2119Constraint.from(RULES).stream().allMatch(c -> c.keyword().equals("MUST")),
            "Anton's thirteen are all MUST, so obligation-aware aggregation changes nothing here");
        assertNull(judge().score(), "8 of 13 is a count of outcomes, never a rating");
    }

    private static Judgment judge() {
        Path workspace = Candidate.workspace();
        return Rfc2119Judge.create("architectural-constraints", Rfc2119Constraint.from(RULES), JudgeBackends.forRecording(workspace, "architecture-rules")).judge(Candidate.contextFor(workspace));
    }
}
