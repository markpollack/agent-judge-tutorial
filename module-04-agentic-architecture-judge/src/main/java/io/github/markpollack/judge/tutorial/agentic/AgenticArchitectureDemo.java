/*
 * Module 04: let the judge investigate
 *
 * Module 03 handed the judge two files and a diff, which means a human had
 * already decided what the relevant convention was. That decision is most of the
 * work, and on a codebase you do not know you cannot make it.
 *
 * Here the judge gets a workspace and tools instead. It has to find the
 * convention itself, say how much code it read to conclude that, and name what
 * departs from it.
 *
 * Same judge, same classifier, same Judgment. Only the prompt changed, and what
 * the prompt asks for is evidence rather than an opinion.
 *
 * Run:               ./mvnw exec:java -pl module-04-agentic-architecture-judge
 * With a real agent: AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.agentic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.architecture.JudgeBackends;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class AgenticArchitectureDemo {

    private static final String CONTROLLER =
        "src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java";

    /** The line the convention ends on: hand the page to the shared helper. */
    private static final String CONVENTIONAL = "return addPaginationModel(page, model, ownersResults);";

    /**
     * The seeded exception: the same feature, refusing to reuse the shared helper and
     * inventing its own model contract instead.
     *
     * <p>Small, unmistakable, and checkable by eye in a live demo. Applied before the second
     * run and reverted immediately after, so the fixture is never left dirty.
     *
     * <p>Indentation is taken from the file rather than written here. PetClinic indents with
     * tabs, and a hard-coded indent silently failed to match, which is why the seed now
     * asserts that it changed something.
     */
    private static String seeded(String indent) {
        return "model.addAttribute(\"currentPage\", page);\n"
            + indent + "model.addAttribute(\"pageCount\", ownersResults.getTotalPages());\n"
            + indent + "model.addAttribute(\"owners\", ownersResults.getContent());\n"
            + indent + "return \"owners/ownersList\";";
    }

    /**
     * Replace the convention's last line inside the city handler only.
     *
     * <p>The same line appears in the last-name handler, which must not be touched: the point
     * of the seed is one handler departing from a convention the other still follows.
     */
    private static String applySeed(String source) {
        int handler = source.indexOf("processFindByCityForm");
        if (handler < 0) {
            throw new IllegalStateException("Cannot seed: no city handler in the controller");
        }
        int target = source.indexOf(CONVENTIONAL, handler);
        if (target < 0) {
            throw new IllegalStateException("Cannot seed: the city handler does not end as expected");
        }
        int lineStart = source.lastIndexOf('\n', target) + 1;
        String indent = source.substring(lineStart, target);
        return source.substring(0, target) + seeded(indent) + source.substring(target + CONVENTIONAL.length());
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== Module 04: Let the judge investigate ===\n");
        System.out.println("Backend: " + JudgeBackends.describe() + "\n");

        Path workspace = PetClinic.candidateWorkspace();

        System.out.println("No evidence is supplied this time. The judge gets the workspace");
        System.out.println("and has to find the convention itself.\n");
        judge(workspace, "agentic-architecture-clean", "The change as the agent left it");

        // ------------------------------------------------------------------
        // Seed one exception and ask again.
        // ------------------------------------------------------------------
        Path controller = workspace.resolve(CONTROLLER);
        String original = Files.readString(controller);
        String seeded = applySeed(original);
        if (seeded.equals(original)) {
            throw new IllegalStateException("Cannot seed: nothing changed, so the run would prove nothing");
        }
        try {
            Files.writeString(controller, seeded);
            System.out.println("\nSeeded one exception: the city search now builds its own model");
            System.out.println("instead of reusing addPaginationModel, with different attribute names.\n");
            judge(workspace, "agentic-architecture-seeded", "The same change, with one seeded exception");
        }
        finally {
            Files.writeString(controller, original);
            System.out.println("\n  (seed reverted, workspace restored)");
        }

        para("""
            A judge that only ever returns PASS is a constant. This one was watched
            finding a departure it was never told to look for, in code it had to go
            and read.

            Note what the judgment carries beyond a verdict: the pattern it found,
            and the population it examined to conclude that. A claim about "the
            dominant convention" drawn from one file is not a finding, and without a
            denominator you cannot tell the difference.

            Next: build, coverage, and conformance are three separate questions. What
            happens when you have to answer all of them at once?
            """);

        System.out.println("Done.");
    }

    private static void judge(Path workspace, String recording, String label) {
        System.out.println("--- " + label + " ---\n");
        long started = System.currentTimeMillis();
        Judgment judgment = InvestigatingArchitectureJudge.create(workspace, recording).judge(workspace(workspace));
        long seconds = (System.currentTimeMillis() - started) / 1000;

        System.out.println("  architectural-conformance  " + judgment.status() + "   (" + seconds + "s)");
        Object pattern = judgment.metadata().get("dominantPattern");
        Object population = judgment.metadata().get("population");
        if (pattern != null) {
            System.out.println("  pattern");
            wrap(pattern.toString());
        }
        if (population != null) {
            System.out.println("  examined");
            wrap(population.toString());
        }
        System.out.println("  finding");
        wrap(judgment.reasoning());
        System.out.println();
        judgment.checks().forEach(check ->
            System.out.printf("    %-4s  %s%n", check.passed() ? "PASS" : "FAIL", check.name()));
        judgment.checks().stream().filter(c -> !c.passed()).forEach(check -> {
            System.out.println("\n  " + check.name() + ":");
            wrap(check.message());
        });
        System.out.println();
    }

    private static io.github.markpollack.judge.context.JudgmentContext workspace(Path path) {
        return PetClinic.contextFor(path);
    }

    private static void wrap(String text) {
        StringBuilder line = new StringBuilder("    ");
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 76) {
                System.out.println(line.toString().stripTrailing());
                line = new StringBuilder("    ");
            }
            line.append(word).append(' ');
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
