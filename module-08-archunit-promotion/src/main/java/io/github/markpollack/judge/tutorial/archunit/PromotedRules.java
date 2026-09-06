package io.github.markpollack.judge.tutorial.archunit;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * The architectural findings that earned promotion into deterministic policy.
 *
 * <p>Two of thirteen. That ratio is the module, not a disappointment: converting every AI
 * observation into a rule produces rules that are vacuous, wrong, or unmaintainable, and the
 * design document that generated these findings says so itself.
 *
 * <p>A rule belongs here when three things are true. It must be decidable from bytecode, so the
 * check is exact rather than a reading. It must have caught something real, so the rule is
 * watched red on actual material rather than on a fixture written to make it fire. And its
 * mechanised form must cover the finding rather than a convenient shadow of it — the test that
 * disqualifies most candidates, and the subject of {@link ShadowRules}.
 */
public final class PromotedRules {

    private PromotedRules() {
    }

    /**
     * The packages the design document governs.
     *
     * <p>Scope is not a detail. The first draft of the time rule ran unscoped and reported 16
     * violations, three of them {@code LocalDate.now()} calls in PetClinic's original owner and
     * visit code, which predates this feature and which RULE-2 says nothing about. An
     * architecture test that reports findings nobody intends to fix is an architecture test
     * somebody eventually deletes, and the deletion takes the real findings with it.
     */
    static final String[] FEATURE_PACKAGES = {
        "org.springframework.samples.petclinic.scheduling..",
        "org.springframework.samples.petclinic.security.."
    };

    /**
     * RULE-2, the part of it a compiler-level tool can decide.
     *
     * <p>"Derive all current time from injected {@code Clock} and validated {@code ZoneId} beans."
     * A call to {@code Instant.now()} is a static read of the system clock, and a class making one
     * is not deriving time from an injected anything. That is a fact about bytecode.
     *
     * <p>What it does not cover: whether the {@code ZoneId} was validated, whether the DST
     * single-offset check is applied, or whether the {@code Clock} that was injected is the right
     * one. Those stay with the judge. This rule is the cheap, exact fraction.
     */
    public static final ArchRule TIME_COMES_FROM_A_CLOCK = noClasses()
        .that().resideInAnyPackage(FEATURE_PACKAGES)
        .should().callMethod(Instant.class, "now")
        .orShould().callMethod(LocalDate.class, "now")
        .orShould().callMethod(LocalDateTime.class, "now")
        .because("RULE-2 requires current time to be derived from an injected Clock, and a static "
            + "now() call is by definition not derived from anything injectable");

    /**
     * RULE-1, the part of it a compiler-level tool can decide.
     *
     * <p>"Centralized enum transition policies; no independent workflow transitions." A public
     * setter for a status enum on a persistent aggregate is the mechanism by which every
     * independent transition in this codebase is performed. Remove the setter and the inline
     * transitions stop compiling.
     *
     * <p>What it does not cover: whether the centralized policy that replaces it is correct, or
     * even whether one exists. A codebase can satisfy this rule perfectly and still re-encode its
     * transition table in six places behind a differently-named method. That is the judge's
     * finding and it stays the judge's.
     */
    public static final ArchRule AGGREGATES_HAVE_NO_PUBLIC_STATUS_SETTER = noMethods()
        .that().areDeclaredInClassesThat().resideInAnyPackage(FEATURE_PACKAGES)
        .and().areDeclaredInClassesThat().areAnnotatedWith("jakarta.persistence.Entity")
        .and().haveName("setStatus")
        .should().bePublic()
        .because("RULE-1 requires transitions to go through a centralized policy, and a public "
            + "status setter on the aggregate is how every inline transition here bypasses one");

    /** Both promoted rules, in the order the design document lists them. */
    public static List<NamedRule> all() {
        return List.of(
            new NamedRule("RULE-1", "aggregates have no public status setter",
                AGGREGATES_HAVE_NO_PUBLIC_STATUS_SETTER),
            new NamedRule("RULE-2", "current time comes from a Clock", TIME_COMES_FROM_A_CLOCK));
    }

    /** An ArchUnit rule with the design rule it was promoted from still attached. */
    public record NamedRule(String ruleId, String description, ArchRule rule) {
    }
}
