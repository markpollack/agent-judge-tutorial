package io.github.markpollack.judge.tutorial.archunit;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.tutorial.build.PetClinic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The claims module 08 makes, pinned.
 *
 * <p>The module asserts that two of thirteen rules deserved promotion, that the mechanisable
 * forms of two others do not capture what the judge found, and that two more are deterministic
 * without being ArchUnit's business. Those are empirical claims about a pinned candidate, so
 * they are tested rather than narrated. If a rule is later loosened until it stops firing, or
 * tightened until it fires on the legacy code it does not govern, these go red.
 */
class PromotionCarveTest {

    private static final Path CANDIDATE = PetClinic.largeCandidate();

    @Test
    void theTimeRuleCatchesTheThirteenTheJudgeFound() {
        List<String> violations = ArchUnitJudge.violations(CANDIDATE,
            PromotedRules.all().stream().filter(r -> r.ruleId().equals("RULE-2")).findFirst().orElseThrow());

        assertEquals(13, violations.size(),
            "RULE-2 was promoted because it caught 13 real static clock reads");
        assertTrue(violations.stream().anyMatch(v -> v.contains("UserPrincipal")),
            "the judge cited UserPrincipal:93 specifically");
    }

    @Test
    void theTimeRuleDoesNotReachIntoCodeItDoesNotGovern() {
        // The unscoped first draft reported three LocalDate.now() calls in PetClinic's original
        // owner and visit code. RULE-2 governs the scheduling feature. An architecture test that
        // reports findings nobody intends to fix is one somebody eventually deletes.
        List<String> violations = ArchUnitJudge.violations(CANDIDATE,
            PromotedRules.all().stream().filter(r -> r.ruleId().equals("RULE-2")).findFirst().orElseThrow());

        assertFalse(violations.stream().anyMatch(v -> v.contains(".owner.")),
            "legacy owner code is out of scope: " + violations);
    }

    @Test
    void theStatusSetterRuleCatchesTheFiveAggregates() {
        List<String> violations = ArchUnitJudge.violations(CANDIDATE,
            PromotedRules.all().stream().filter(r -> r.ruleId().equals("RULE-1")).findFirst().orElseThrow());

        assertEquals(5, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.contains("SchedulingRequest")),
            "the judge cited SchedulingRequest.setStatus specifically");
    }

    @Test
    void theLockOrderShadowIsGreenOnCodeTheJudgeFailed() {
        // RULE-4's mechanisable half -- no solver work inside a transaction -- holds. The half
        // that was violated is the acquisition order, which is a runtime sequence that bytecode
        // dependency analysis cannot express. Promoting this rule would have replaced a true
        // finding with a green check.
        List<String> violations = ArchUnitJudge.violations(CANDIDATE,
            ShadowRules.all().stream().filter(r -> r.ruleId().equals("RULE-4")).findFirst().orElseThrow());

        assertTrue(violations.isEmpty(), "expected the shadow to be green, got: " + violations);
    }

    @Test
    void theEntityBoundaryShadowFindsTheWrongThings() {
        // Worse than green. It reports two private helpers returning an Owner internally, which
        // RULE-8 permits, and never sees Model.addAttribute, whose second parameter erases to
        // Object and where the actual violation lives.
        List<String> violations = ArchUnitJudge.violations(CANDIDATE,
            ShadowRules.all().stream().filter(r -> r.ruleId().equals("RULE-8")).findFirst().orElseThrow());

        assertEquals(2, violations.size(), violations.toString());
        assertTrue(violations.stream().allMatch(v -> v.contains("getAuthenticatedOwner")),
            "every one is a false positive: " + violations);
        assertFalse(violations.stream().anyMatch(v -> v.contains("addAttribute")),
            "the real violation is invisible to this rule");
    }

    @Test
    void theConfigRulesAnswerWithoutArchUnit() {
        Check flyway = ConfigRules.flywayOwnsTheSchema(CANDIDATE);
        Check matrix = ConfigRules.mavenOwnsTheDatabaseMatrix(CANDIDATE);

        assertTrue(flyway.passed(), flyway.message());
        assertFalse(matrix.passed(), "the judge found maven-build.yml runs only a java matrix");
        assertTrue(matrix.message().contains("mysql"), matrix.message());
    }

    @Test
    void theCarveAddsUp() {
        // Thirteen rules, four outcomes. The count is the module's headline and it is the kind
        // of number that drifts when somebody adds a rule and forgets the prose.
        assertEquals(2, PromotedRules.all().size(), "promoted to ArchUnit");
        assertEquals(2, ShadowRules.all().size(), "shown as shadows rather than promoted");
    }
}
