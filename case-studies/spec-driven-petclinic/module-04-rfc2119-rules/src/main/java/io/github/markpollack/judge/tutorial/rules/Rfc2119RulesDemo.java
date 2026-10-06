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

import io.github.markpollack.judge.judgment.JudgmentStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.markpollack.judge.judgment.Check;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.Rfc2119Requirement;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.Rfc2119Jury;
import io.github.markpollack.judge.verdict.Verdict;
import io.github.markpollack.judge.jury.Jury;
import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.tutorial.support.RosterResults;

public class Rfc2119RulesDemo {

    private static final Path RULES = Candidate.SPEC.resolve("rules.md");

    private static final Pattern LOCATION =
        Pattern.compile("[A-Za-z0-9_/.-]*[A-Za-z0-9_-]+\\.(?:java|xml|sql|html|yml|yaml|properties)(?::[\\d-]+)?");


    /** Configure one whole-roster investigation. Workspace/tools belong to the runtime. */
    public static Jury jury(EvalModel runtime) {
        return Rfc2119Jury.builder().runtime(runtime).requirements(Rfc2119Requirement.from(RULES, "petclinic:fc9df4af")).build();
    }

    public static void main(String[] args) {
        System.out.println("=== Module 04: Run the architectural rules ===\n");
        System.out.println("The behavioural specification mostly held.");
        System.out.println("But the same author also wrote architectural requirements,");
        System.out.println("before the code, in the same repository.\n");

        List<Rfc2119Requirement> constraints = Rfc2119Requirement.from(RULES, "petclinic:fc9df4af");
        Path workspace = Candidate.workspace();

        System.out.println("  " + constraints.size() + " required MUSTs\n");

        Verdict verdict = jury(JudgeBackends.forRecording(workspace, "architecture-rules")).vote();
        Judgment judgment = verdict.judgment();

        Map<String, Rfc2119Requirement> byId = constraints.stream()
            .collect(Collectors.toMap(Rfc2119Requirement::id, c -> c, (a, b) -> a));
        long held = RosterResults.checks(verdict).stream().filter(c -> c.judgment().status() == JudgmentStatus.PASS).count();
        List<Check> violated = RosterResults.checks(verdict).stream().filter(c -> c.judgment().status() == JudgmentStatus.FAIL).toList();

        System.out.printf("  %2d PASS%n", held);
        System.out.printf("  %2d FAIL%n%n", violated.size());
        System.out.println("  Overall: " + verdict.conclusion() + "\n");

        if (!violated.isEmpty()) {
            System.out.println("  Failed requirements:");
            violated.forEach(check -> {
                Rfc2119Requirement constraint = byId.get(check.id());
                System.out.println();
                System.out.println("    " + pad(check.id())
                    + (constraint == null ? "" : constraint.specification().requirement()));
                locations(check.judgment().reasoning()).forEach(location ->
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
