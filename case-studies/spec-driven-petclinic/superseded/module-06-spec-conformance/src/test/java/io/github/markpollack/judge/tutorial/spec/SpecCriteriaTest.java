package io.github.markpollack.judge.tutorial.spec;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rubric reader decides the denominator, so it is worth testing on its own.
 * A reader that silently finds 48 of 52 would understate what the judge failed to answer.
 */
class SpecCriteriaTest {

    private static final Path UC6 = Path.of("fixtures/petclinic/appointment-scheduling-spec-with-usecases",
        "spec/smart-appointment-scheduling/manage-appointment-lifecycle/criteria.md");

    @Test
    void readsEveryAcceptanceCriterionInTheUseCase() {
        assertEquals(52, SpecCriteria.from(UC6).size());
    }

    @Test
    void preservesTheSpecificationsOwnIdentifiers() {
        List<SpecCriteria> criteria = SpecCriteria.from(UC6);
        assertAll(
            () -> assertEquals("UC6-AC1", criteria.get(0).id()),
            () -> assertTrue(criteria.stream().allMatch(c -> c.id().startsWith("UC6-AC")),
                "every id should come from the specification"),
            () -> assertEquals(52, criteria.stream().map(SpecCriteria::id).distinct().count(),
                "ids must be unique, or the judge cannot say which criterion it answered"));
    }

    @Test
    void carriesTheRequirementSentenceNotJustTheTitle() {
        SpecCriteria adjacent = SpecCriteria.from(UC6).stream()
            .filter(c -> c.id().equals("UC6-AC5"))
            .findFirst()
            .orElseThrow();

        assertAll(
            () -> assertEquals("Permit adjacent future appointments", adjacent.title()),
            () -> assertTrue(adjacent.requirement().contains("shall not treat the half-open intervals"),
                "the requirement text is what the judge assesses: " + adjacent.requirement()));
    }
}
