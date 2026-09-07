/*
 * Module 05: Investigate a failure.
 *
 * Module 04 ended with eight failed requirements, each carrying a location.
 * That is an address. It is not yet something anyone can act on, because it
 * does not say what goes wrong or whether it can happen.
 *
 * One investigation, not eight. The concept is the tier boundary, and the
 * eighth repetition of it teaches nothing the first did not.
 *
 * Run:               ./mvnw exec:java -pl module-05-investigation
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.investigation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.Investigation;
import io.github.markpollack.judge.tutorial.support.Investigator;
import io.github.markpollack.judge.tutorial.support.Rfc2119Constraint;
import io.github.markpollack.judge.tutorial.support.Rfc2119Judge;

public class InvestigationDemo {

    private static final Path RULES = Candidate.SPEC.resolve("rules.md");

    /** The lock-ordering rule. Chosen because its consequence is a real class of defect. */
    private static final String RULE = "RULE-4";

    private static final String RECORDING = "rule-4-investigation";

    /** Locations shown on stage. The rest stay on the record, which is where they belong. */
    private static final int SHOWN = 4;

    private static final Pattern LOCATION =
        Pattern.compile("[A-Za-z0-9_/.-]*[A-Za-z0-9_-]+\\.(?:java|xml|sql|html|yml|yaml|properties)(?::[\\d-]+)?");

    public static void main(String[] args) {
        System.out.println("=== Module 05: Investigate a failure ===\n");
        System.out.println("Module 04 produced eight failed requirements.");
        System.out.println("Each one is an address. None of them is yet a consequence.\n");

        List<Rfc2119Constraint> constraints = Rfc2119Constraint.from(RULES);
        Path workspace = Candidate.workspace();

        // The lead comes out of module 04's actual judgment. Nothing is copied by hand:
        // if the judge stopped reporting RULE-4, this module would stop having an input,
        // which is the correct behaviour for a tier that consumes another tier's output.
        Judgment judgment = Rfc2119Judge
            .create("architectural-constraints", workspace, constraints, "architecture-rules")
            .judge(Candidate.contextFor(workspace));

        Map<String, Rfc2119Constraint> byId = constraints.stream()
            .collect(Collectors.toMap(Rfc2119Constraint::id, c -> c, (a, b) -> a));

        Optional<Check> lead = judgment.checks().stream()
            .filter(check -> RULE.equals(check.name()) && !check.passed())
            .findFirst();

        if (lead.isEmpty() || !byId.containsKey(RULE)) {
            System.out.println("  " + RULE + " is not a failed requirement in this judgment.");
            System.out.println("  There is nothing to investigate, and that is not an error.");
            System.out.println("\nDone.");
            return;
        }

        Rfc2119Constraint constraint = byId.get(RULE);
        Check check = lead.get();

        System.out.println("  Take one of them.\n");
        System.out.println("    " + RULE + "   FAIL");
        System.out.println("    " + " ".repeat(RULE.length()) + "   " + constraint.title());
        locations(check.message()).forEach(location ->
            System.out.println("    " + " ".repeat(RULE.length()) + "   " + location));
        System.out.println();
        System.out.println("  The judge answered the question it was asked: does the rule hold?");
        System.out.println("  It was never asked what happens if it doesn't.\n");

        Optional<Investigation> found;
        try {
            found = Investigator.investigate(constraint, check, workspace, RECORDING);
        }
        catch (IllegalStateException problem) {
            // DD-8: an investigation that could not run is a problem with the instrument,
            // and saying so is not the same as saying the subject is fine.
            System.out.println("  Could not investigate: " + problem.getMessage());
            System.out.println("\nDone.");
            return;
        }

        if (found.isEmpty()) {
            System.out.println("  The investigation did not answer the question it was asked.");
            System.out.println("\nDone.");
            return;
        }

        Investigation investigation = found.get();

        System.out.println("  So ask that, separately.\n");

        System.out.println("    Consequence");
        wrap(investigation.consequence());
        System.out.println();

        System.out.println("    Reachable        " + investigation.reachability());
        wrap(investigation.reachabilitySummary());
        System.out.println();

        List<String> established = investigation.locations().stream()
            .map(location -> location.replaceAll("src/(main|test)/java/(?:[A-Za-z0-9_]+/)+", ""))
            .toList();
        if (!established.isEmpty()) {
            System.out.println("    Established at");
            // First line per distinct file. Four consecutive lines of one class would show the
            // near side of the deadlock four times and its counterparty not at all; the point of
            // an ordering violation is that it takes two places to make one.
            shown(established).forEach(location -> System.out.println("      " + location));
            int rest = established.size() - shown(established).size();
            if (rest > 0) {
                System.out.println("      and " + rest
                    + " more, with the full argument, in the recording");
            }
            System.out.println();
        }

        System.out.println("    Outcome          " + investigation.outcome()
            + (investigation.changedTheLead() ? "   (the lead changed)" : ""));
        System.out.println();

        // Shown rather than narrated. The judge cited one line and the investigation cites
        // another, and printing both is the whole argument for why the second tier is a
        // separate call: it read past the address it was given and moved it.
        List<String> leadLocations = locations(check.message());
        if (!established.isEmpty() && !leadLocations.isEmpty()
                && !established.get(0).equals(leadLocations.get(0))) {
            System.out.println("    It moved the address");
            System.out.println("      judge            " + leadLocations.get(0));
            System.out.println("      investigation    " + established.get(0));
            System.out.println();
        }

        para("""
            Two questions, asked separately, by two calls that were allowed to
            disagree.

            The judge said where. The investigation said what, and whether it
            can happen. Neither could have produced the other's half, and the
            second one was free to come back and say the first overstated it.

            That is the difference between eight complaints and one finding.
            """);
        System.out.println("Done.");
    }

    /** One location per distinct file, up to {@link #SHOWN}, in the order the investigation gave. */
    private static List<String> shown(List<String> locations) {
        List<String> picked = new java.util.ArrayList<>();
        List<String> files = new java.util.ArrayList<>();
        for (String location : locations) {
            String file = location.substring(0, location.lastIndexOf(':'));
            if (!files.contains(file)) {
                files.add(file);
                picked.add(location);
            }
            if (picked.size() == SHOWN) {
                break;
            }
        }
        return List.copyOf(picked);
    }

    /** A failure is worth what you can open. Keep the file and line, drop the package ceremony. */
    private static List<String> locations(String message) {
        Matcher matcher = LOCATION.matcher(message == null ? "" : message);
        return matcher.results()
            .map(result -> result.group().replaceAll("src/(main|test)/java/(?:[A-Za-z0-9_]+/)+", ""))
            .distinct()
            .limit(2)
            .toList();
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("      ");
        for (String word : text.strip().split("\\s+")) {
            if (line.length() + word.length() > 76 && line.length() > 6) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("      ");
            }
            line.append(word).append(' ');
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static void para(String text) {
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
