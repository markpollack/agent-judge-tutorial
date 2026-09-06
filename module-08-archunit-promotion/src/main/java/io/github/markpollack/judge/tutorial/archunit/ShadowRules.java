package io.github.markpollack.judge.tutorial.archunit;

import java.util.List;

import com.tngtech.archunit.lang.ArchRule;

import io.github.markpollack.judge.tutorial.archunit.PromotedRules.NamedRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * The rules you would write for findings that should not have been promoted.
 *
 * <p>Every one of these is a real ArchUnit rule, correctly expressing something the design
 * document actually says, and every one of them passes on code the judge correctly failed. They
 * are not strawmen — they are the rule a competent engineer writes when told to mechanise the
 * finding, because they mechanise the part of the finding that bytecode can see.
 *
 * <p>That is the trap. The finding was severe, the rule is green, and the green rule is now the
 * thing the team looks at. A judgment that has been replaced by a check that cannot express it
 * has not been promoted; it has been lost, and the reassuring green makes the loss hard to
 * notice. Which is why these live here, run alongside the promoted rules, and are asserted to
 * pass — so the module can show the gap rather than assert it.
 */
public final class ShadowRules {

    private ShadowRules() {
    }

    /**
     * RULE-8's shadow. The judge found five JPA entities bound straight into the views.
     *
     * <p>The mechanisable form asks whether a controller method returns an entity type. Spring
     * MVC controllers return a {@code String} view name and pass the entity through
     * {@code Model.addAttribute(String, Object)}, whose second parameter erases to
     * {@code Object}. The value's type is a runtime fact and the rule cannot see it.
     */
    public static final ArchRule CONTROLLERS_DO_NOT_RETURN_ENTITIES = noMethods()
        .that().areDeclaredInClassesThat().resideInAnyPackage(PromotedRules.FEATURE_PACKAGES)
        .and().areDeclaredInClassesThat().areAnnotatedWith("org.springframework.stereotype.Controller")
        .should().haveRawReturnType(
            com.tngtech.archunit.base.DescribedPredicate.describe("a JPA entity",
                javaClass -> javaClass.isAnnotatedWith("jakarta.persistence.Entity")))
        .because("RULE-8 requires role-specific DTOs at the web boundary rather than serialized "
            + "JPA entities -- but this only sees return types, and MVC returns a view name");

    /**
     * RULE-4's shadow. The judge found the mandated lock order inverted in two methods.
     *
     * <p>RULE-4 has two halves. "No external AI or solver work while a lock is open" is a
     * dependency fact and is checkable. "Locks must be acquired in this global order" is a claim
     * about the sequence of two calls at runtime, and bytecode dependency analysis has no notion
     * of order. The checkable half is the half that was not violated.
     */
    public static final ArchRule NO_SOLVER_WORK_INSIDE_A_TRANSACTION = noClasses()
        .that().resideInAPackage("org.springframework.samples.petclinic.scheduling.service..")
        .should().dependOnClassesThat().resideInAnyPackage("ai.timefold..")
        .because("RULE-4 forbids solver work while a transaction or lock is open -- but this "
            + "sees only which packages a class reaches, never in what order it does anything");

    /** Both shadows, each named for the design rule it fails to capture. */
    public static List<NamedRule> all() {
        return List.of(
            new NamedRule("RULE-4", "no solver work inside a transaction", NO_SOLVER_WORK_INSIDE_A_TRANSACTION),
            new NamedRule("RULE-8", "controllers do not return entities", CONTROLLERS_DO_NOT_RETURN_ENTITIES));
    }
}
