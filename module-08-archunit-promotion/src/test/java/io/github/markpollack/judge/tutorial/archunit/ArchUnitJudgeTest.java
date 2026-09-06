package io.github.markpollack.judge.tutorial.archunit;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.lang.ArchRule;

import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;
import io.github.markpollack.judge.tutorial.archunit.PromotedRules.NamedRule;
import io.github.markpollack.judge.tutorial.build.PetClinic;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a set of ArchUnit outcomes means.
 *
 * <p>A deterministic judge is not exempt from the questions an AI-backed one has to answer. It
 * can still pass over nothing, still lose the parts on the way to a verdict, and still report a
 * green that examined no classes. Assertions are on {@code status()}, never {@code pass()}.
 */
class ArchUnitJudgeTest {

    private static final ArchRule ALWAYS_HOLDS = classes()
        .should().haveNameMatching(".*").because("a tautology, for testing the rollup");

    private static final ArchRule NEVER_HOLDS = noClasses()
        .should().haveNameMatching(".*").because("a contradiction, for testing the rollup");

    @Test
    void everyRuleHoldingIsAPass() {
        Judgment judgment = ArchUnitJudge.judge(PetClinic.largeCandidate(),
            List.of(new NamedRule("RULE-X", "a tautology", ALWAYS_HOLDS)));

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals(1, judgment.checks().size());
    }

    @Test
    void oneViolatedRuleFailsTheWhole() {
        // Conjunctive, like every definition of done in this tutorial. One rule out of two
        // holding is not half-conformant.
        Judgment judgment = ArchUnitJudge.judge(PetClinic.largeCandidate(), List.of(
            new NamedRule("RULE-X", "a tautology", ALWAYS_HOLDS),
            new NamedRule("RULE-Y", "a contradiction", NEVER_HOLDS)));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
        assertEquals(2, judgment.checks().size(), "both outcomes are evidence and must be kept");
        assertTrue(judgment.reasoning().startsWith("1 of 2 promoted rules hold"), judgment.reasoning());
    }

    @Test
    void anEmptyRuleSetIsAnErrorNotAPass() {
        // Every rule in an empty set holds. That is true and it is not a verdict about anything.
        Judgment judgment = ArchUnitJudge.judge(PetClinic.largeCandidate(), List.of());
        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    @Test
    void anUnbuiltCandidateIsRefusedRatherThanPassed() {
        // ArchUnit over an empty import passes every rule. The judge must not reach that state:
        // an unbuilt workspace is the operator's problem, not a finding about the code.
        assertThrows(IllegalStateException.class,
            () -> ArchUnitJudge.judge(Path.of("/nonexistent"), PromotedRules.all()));
    }

    @Test
    void theDenominatorIsReported() {
        Judgment judgment = ArchUnitJudge.judge(PetClinic.largeCandidate(),
            List.of(new NamedRule("RULE-X", "a tautology", ALWAYS_HOLDS)));

        assertEquals(1, judgment.metadata().get("rulesRun"));
        assertTrue((int) judgment.metadata().get("classesExamined") > 100,
            "how many classes a rule examined is the difference between 'holds' and 'matched nothing'");
        assertTrue(judgment.reasoning().contains("classes"), judgment.reasoning());
    }

    @Test
    void aViolationReportsWhereToLook() {
        Judgment judgment = ArchUnitJudge.judge(PetClinic.largeCandidate(),
            List.of(new NamedRule("RULE-1", "no public status setter",
                PromotedRules.AGGREGATES_HAVE_NO_PUBLIC_STATUS_SETTER)));

        String message = judgment.checks().get(0).message();
        assertTrue(message.contains("violation(s), first at"), message);
        assertTrue(message.contains(".java:"), "the binding item must point at a file and line: " + message);
    }
}
