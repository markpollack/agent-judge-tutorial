/*
 * Module 04: Run the architectural rules.
 *
 * Same generated system. Same judge shape. Another document written before the
 * code -- and a different answer.
 *
 * The acceptance criteria said what the system must do. This says how it must
 * be built. Nothing about the subject changed between module 03 and here.
 *
 * Run:               ./mvnw exec:java -pl module-04-rfc2119-rules
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.rules;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.Rfc2119Constraint;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.Rfc2119Judge;

public class Rfc2119RulesDemo {

    private static final Path RULES = Candidate.SPEC.resolve("rules.md");

    private static final Pattern LOCATION =
        Pattern.compile("[A-Za-z0-9_/.-]*[A-Za-z0-9_-]+\\.(?:java|xml|sql|html|yml|yaml|properties)(?::[\\d-]+)?");

    public static void main(String[] args) {
        System.out.println("=== Module 04: Run the architectural rules ===\n");
        System.out.println("The behavioural specification mostly held.");
        System.out.println("But the same author also wrote architectural requirements,");
        System.out.println("before the code, in the same repository.\n");

        List<Rfc2119Constraint> constraints = Rfc2119Constraint.from(RULES);
        Path workspace = Candidate.workspace();

        System.out.println("  " + constraints.size() + " required MUSTs\n");

        Judgment judgment = Rfc2119Judge
            .create("architectural-constraints", constraints, JudgeBackends.forRecording(workspace, "architecture-rules"))
            .judge(Candidate.contextFor(workspace));

        Map<String, Rfc2119Constraint> byId = constraints.stream()
            .collect(Collectors.toMap(Rfc2119Constraint::id, c -> c, (a, b) -> a));
        long held = judgment.checks().stream().filter(Check::passed).count();
        List<Check> violated = judgment.checks().stream().filter(c -> !c.passed()).toList();

        System.out.printf("  %2d PASS%n", held);
        System.out.printf("  %2d FAIL%n%n", violated.size());
        System.out.println("  Overall: " + judgment.status() + "\n");

        if (!violated.isEmpty()) {
            System.out.println("  Failed requirements:");
            violated.forEach(check -> {
                Rfc2119Constraint constraint = byId.get(check.name());
                System.out.println();
                System.out.println("    " + pad(check.name())
                    + (constraint == null ? "" : constraint.title()));
                locations(check.message()).forEach(location ->
                    System.out.println("    " + pad("") + location));
            });
            System.out.println();
        }

        para("""
            Same generated system. Another document written before the code.
            A different answer.

            The build passed. The behavioural requirements mostly held. The
            architectural specification did not.

            Each of those is an address, not yet a consequence. Module 05 asks
            what they actually mean and whether they are reachable.
            """);
        System.out.println("Done.");
    }

    private static String pad(String id) {
        return (id + "          ").substring(0, 10);
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

    private static void para(String text) {
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
