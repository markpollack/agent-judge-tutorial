/*
 * Module 02: Run the spec.
 *
 * The build passed and told us nothing about whether the agent did what was
 * asked. But somebody wrote down what was asked, numbered, before any code
 * existed -- and that document is still sitting in the repository.
 *
 * This module reads six of those requirements verbatim and asks the
 * implementation to answer for them.
 *
 * Run:               ./mvnw exec:java -pl module-02-ears-slice
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Path;
import java.util.List;

import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.ai.requirements.EarsCriterion;
import io.github.markpollack.judge.tutorial.support.JudgeBackends;
import io.github.markpollack.judge.ai.requirements.EarsJudge;
import io.github.markpollack.judge.ai.requirements.Observation;

public class EarsSliceDemo {

    private static final Path CRITERIA =
        Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md");

    /** The owner-cancellation slice: one feature, six requirements, readable in one breath. */
    private static final String[] SLICE =
        { "UC6-AC7", "UC6-AC8", "UC6-AC9", "UC6-AC10", "UC6-AC11", "UC6-AC12" };

    public static void main(String[] args) {
        System.out.println("=== Module 02: Run the spec ===\n");
        System.out.println("The implementation builds.");
        System.out.println("But did it do what was asked?\n");

        List<EarsCriterion> all = EarsCriterion.from(CRITERIA);
        List<EarsCriterion> slice = EarsCriterion.select(all, SLICE);

        System.out.println("Somebody wrote that down before the code existed:");
        System.out.println("  " + CRITERIA);
        System.out.println("  " + all.size() + " numbered requirements. Here are six of them.\n");

        slice.forEach(c -> {
            System.out.println("  " + padId(c.id()) + c.title());
            wrap(c.requirement());
        });
        System.out.println();

        Path workspace = Candidate.workspace();
        Judgment judgment = EarsJudge
            .create("appointment-cancellation", slice, JudgeBackends.forRecording(workspace, "ears-uc6-cancellation"))
            .judge(Candidate.contextFor(workspace));

        System.out.println();
        judgment.checks().forEach(check ->
            System.out.println("  " + pad(check.name()) + (check.passed() ? "PASS" : "FAIL")));
        System.out.println();
        System.out.println("  Overall: " + judgment.status());
        System.out.println("  " + judgment.reasoning());
        List<Observation> observations = Observation.of(judgment);
        if (!observations.isEmpty()) {
            System.out.println();
            System.out.println("  Also noticed, changing nothing above:");
            observations.forEach(o -> labelled(o.requirementId(), lead(o)));
        }
        System.out.println();

        para("""
            These are not criteria the judge invented. They are the specification's
            own requirements, numbered by somebody else, written before the
            implementation existed.

            That is the whole idea. A spec-driven project already produces the
            evaluation surface; nothing was running it as one.

            Six is readable. Module 03 runs all 52.
            """);
        System.out.println("Done.");
    }

    /**
     * Non-binding evidence the judge noticed on the way to a verdict. It changed nothing above:
     * all six requirements passed and the observation takes no part in that.
     */
    /**
     * The lead clause and one location. The full sentence is preserved in the judgment's metadata
     * and verbatim in the recording; the stage does not need all of it.
     *
     * <p>Paths are evidence, not scenery: keep the file and line, drop the package ceremony.
     */
    private static String lead(Observation observation) {
        String message = observation.message().replaceAll("\\s+", " ").strip();
        int stop = message.indexOf(" \u2014 ");
        if (stop < 0) {
            stop = message.indexOf(", ");
        }
        String clause = (stop > 20 ? message.substring(0, stop) : message).replaceAll("\\s*\\(.*", "");
        String location = observation.locations().isEmpty() ? ""
            : "  " + observation.locations().get(0).replaceAll("src/(main|test)/java/(?:[A-Za-z0-9_]+/)+", "");
        return clause + location;
    }

    /** An identifier in the left column, its text wrapped and hanging under itself. */
    private static void labelled(String label, String body) {
        String first = "    " + pad(label);
        String hanging = " ".repeat(first.length());
        StringBuilder line = new StringBuilder(first);
        boolean started = false;
        for (String word : body.strip().split("\\s+")) {
            if (started && line.length() + word.length() > 80) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder(hanging);
            }
            line.append(word).append(' ');
            started = true;
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static String pad(String id) {
        return (id + "            ").substring(0, 12);
    }

    // Titles line up with the requirement text wrap() indents to column 13.
    private static String padId(String id) {
        return (id + "          ").substring(0, 10);
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("            ");
        for (String word : text.strip().split("\\s+")) {
            if (line.length() + word.length() > 78 && line.length() > 12) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("            ");
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
