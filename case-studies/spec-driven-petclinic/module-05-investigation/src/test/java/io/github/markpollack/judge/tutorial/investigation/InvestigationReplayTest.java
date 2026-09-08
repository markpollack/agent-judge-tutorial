package io.github.markpollack.judge.tutorial.investigation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.Investigation;
import io.github.markpollack.judge.tutorial.support.Investigator;
import io.github.markpollack.judge.ai.requirements.Rfc2119Constraint;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.Rfc2119Judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The module, as a build gate rather than a demo.
 *
 * <p>Two different things are asserted here and they are worth separating. The first is that the
 * module's own output is stable on replay, which is what makes it safe to stand in front of. The
 * second is that the investigation's citations are real — every file it named exists and every line
 * it named is inside that file.
 *
 * <p>That second one is the tutorial's own advice turned on itself. The claim running through this
 * case study is that a judge's addresses are checkable and its prose is not; a test that checks the
 * addresses mechanically is what that claim looks like when someone acts on it.
 */
class InvestigationReplayTest {

    private static final Pattern LOCATION = Pattern.compile("^(.*):(\\d+)$");

    private Investigation investigate() {
        List<Rfc2119Constraint> constraints = Rfc2119Constraint.from(Candidate.SPEC.resolve("rules.md"));
        Path workspace = Candidate.workspace();

        Judgment judgment = Rfc2119Judge
            .create("architectural-constraints", constraints, JudgeBackends.forRecording(workspace, "architecture-rules"))
            .judge(Candidate.contextFor(workspace));

        Map<String, Rfc2119Constraint> byId = constraints.stream()
            .collect(Collectors.toMap(Rfc2119Constraint::id, c -> c, (a, b) -> a));

        Check lead = judgment.checks().stream()
            .filter(check -> "RULE-4".equals(check.name()) && !check.passed())
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "module 04 no longer reports RULE-4 as failed, so module 05 has no input"));

        Optional<Investigation> found = Investigator.investigate(byId.get("RULE-4"), lead,
            workspace, "rule-4-investigation");
        assertTrue(found.isPresent(), "the recorded investigation must still parse");
        return found.get();
    }

    /** Tier one's output is tier two's input. If that link breaks, this fails rather than drifts. */
    @Test
    void theLeadComesFromModuleFoursJudgment() {
        assertEquals("RULE-4", investigate().requirementId());
    }

    @Test
    void theRecordedInvestigationReplaysToTheSameAnswer() {
        Investigation investigation = investigate();

        assertEquals(Investigation.Outcome.CONFIRMED, investigation.outcome());
        assertEquals(Investigation.Reachability.REACHABLE, investigation.reachability());
        assertFalse(investigation.consequence().isBlank());
        assertFalse(investigation.reachabilitySummary().isBlank());
    }

    /**
     * The multi-paragraph argument survives parsing. It is the part that names the counterparty
     * paths, which is the whole difference between an assertion and an argument.
     */
    @Test
    void theReachabilityArgumentIsKeptWhole() {
        Investigation investigation = investigate();

        assertTrue(investigation.reachabilityArgument().length()
            > investigation.reachabilitySummary().length(),
            "the stage shows the opening paragraph; the record keeps all of it");
        assertTrue(investigation.reachabilityArgument().contains("LifecycleScheduler")
            || investigation.reachabilityArgument().contains("lifecycle"),
            "the argument must name the counterparty, not merely assert one exists");
    }

    /**
     * The judge cited the method signature; the investigation cited the lock. Reading past the
     * address you were given is the reason the second tier is a separate call, so it is asserted
     * rather than hoped for.
     */
    @Test
    void theInvestigationMovedTheAddress() {
        Investigation investigation = investigate();

        assertNotEquals("StaffFallbackService.java:247", shortest(investigation.locations().get(0)));
        assertEquals("StaffFallbackService.java:248", shortest(investigation.locations().get(0)));
    }

    /**
     * Every claim carries an address, and every address is real.
     *
     * <p>This is the assertion that would have caught a fabricated citation, and it is cheap. A
     * judge whose locations do not resolve is worse than one that says nothing, because its output
     * looks actionable.
     */
    @Test
    void everyCitedLocationExistsInTheSubject() throws IOException {
        Path workspace = Candidate.workspace();
        List<String> locations = investigate().locations();

        assertFalse(locations.isEmpty(), "an investigation with no addresses has not localized anything");

        for (String location : locations) {
            Matcher matcher = LOCATION.matcher(location);
            assertTrue(matcher.matches(), () -> "not a file:line reference: " + location);

            Path file = workspace.resolve(matcher.group(1));
            assertTrue(Files.isRegularFile(file), () -> "cited file does not exist: " + location);

            long lines = Files.lines(file).count();
            int line = Integer.parseInt(matcher.group(2));
            assertTrue(line >= 1 && line <= lines,
                () -> "cited line " + line + " is outside " + matcher.group(1) + " (" + lines + " lines)");
        }
    }

    private static String shortest(String location) {
        return location.replaceAll("src/(main|test)/java/(?:[A-Za-z0-9_]+/)+", "");
    }
}
