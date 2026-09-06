package io.github.markpollack.judge.tutorial.rules;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parser is the denominator. If it silently finds nine rules in a document with thirteen,
 * every downstream count is a lie that nothing else in the pipeline can detect.
 */
class ArchitectureRuleTest {

    private static final Path RULES = Path.of(
        "fixtures/petclinic/appointment-scheduling-spec-with-usecases",
        "spec/smart-appointment-scheduling/rules.md");

    @Test
    void readsEveryRuleInTheDocument() {
        assertEquals(13, ArchitectureRule.from(RULES).size());
    }

    @Test
    void identifiersAreTheDocumentsOwnAndUnique() {
        List<ArchitectureRule> rules = ArchitectureRule.from(RULES);
        Set<String> ids = rules.stream().map(ArchitectureRule::id).collect(Collectors.toSet());
        assertEquals(rules.size(), ids.size(), "duplicate identifiers collapse the denominator");
        assertTrue(ids.contains("RULE-1"));
        assertTrue(ids.contains("RULE-13"));
    }

    @Test
    void carriesTheRequirementAndTheReason() {
        ArchitectureRule two = ArchitectureRule.from(RULES).stream()
            .filter(rule -> rule.id().equals("RULE-2")).findFirst().orElseThrow();

        assertTrue(two.requirement().contains("`Instant`"), two.requirement());
        assertTrue(two.reason().contains("DST rejection"), two.reason());
        assertTrue(two.asPrompt().startsWith("RULE-2: MUST persist"), two.asPrompt());
    }

    @Test
    void labelIsShortEnoughToPrint() {
        ArchitectureRule.from(RULES).forEach(rule ->
            assertTrue(rule.title().length() <= 72, rule.id() + " label too long: " + rule.title()));
    }

    @Test
    void aRuleWithoutARequirementIsNotInvented() {
        // The MUST clause is the rule. A heading with no MUST is a document defect, and the
        // parser must drop it rather than emit a rule whose requirement is whatever line
        // happened to follow.
        List<ArchitectureRule> rules = ArchitectureRule.from(RULES);
        assertFalse(rules.stream().anyMatch(rule -> rule.requirement().isBlank()));
        assertFalse(rules.stream().anyMatch(rule -> rule.requirement().startsWith("**Covers:**")));
    }
}
