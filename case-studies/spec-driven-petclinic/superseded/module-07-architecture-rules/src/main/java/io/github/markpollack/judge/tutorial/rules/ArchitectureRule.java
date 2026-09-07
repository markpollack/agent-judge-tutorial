package io.github.markpollack.judge.tutorial.rules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.markpollack.judge.tutorial.spec.Criterion;

/**
 * One numbered architectural rule from the feature's technical design.
 *
 * <p>These sit one level up from the acceptance criteria of module 06. A criterion says what the
 * system must do; a rule says how the code must be built. "Persist instants as {@code Instant}"
 * and "controllers must not perform workflow transitions" are not statements about behaviour a
 * user could observe. They are statements about structure, and they are the thing that decays
 * quietly when nobody is looking.
 *
 * <h2>The reason travels with the rule</h2>
 *
 * <p>Each rule carries a {@code **Reason:**} clause and this keeps it. That is a deliberate
 * choice with a cost: it lets the judge read intent, so a technically-conforming implementation
 * that defeats the purpose can be marked down, and it equally lets the judge excuse a literal
 * violation it decides was in the spirit of the thing. The reason is part of what the design
 * author wrote down, so it is part of the rule. Module 08 is where that latitude gets taken away
 * from the rules that do not need it.
 */
public record ArchitectureRule(String id, String title, String requirement, String reason) implements Criterion {

    /** {@code ### RULE-1} */
    private static final Pattern HEADING = Pattern.compile("^### (RULE-\\d+)$");

    /**
     * Read every rule out of the feature's {@code rules.md}.
     *
     * <p>The shape is fixed by the document: a heading, a {@code **Covers:**} traceability line,
     * a {@code **MUST**} requirement, and a {@code **Reason:**}. A rule missing its requirement is
     * dropped rather than guessed at, and the count is reported so a parser that quietly finds
     * nine of thirteen cannot pass for a document with nine rules.
     */
    public static List<ArchitectureRule> from(Path rulesFile) {
        List<ArchitectureRule> rules = new ArrayList<>();
        String id = null;
        String requirement = null;

        for (String line : read(rulesFile).lines().toList()) {
            String text = line.strip();
            Matcher heading = HEADING.matcher(text);
            if (heading.matches()) {
                id = heading.group(1);
                requirement = null;
                continue;
            }
            if (id == null) {
                continue;
            }
            if (text.startsWith("**MUST**")) {
                requirement = text.substring("**MUST**".length()).strip();
            }
            else if (text.startsWith("**Reason:**") && requirement != null) {
                rules.add(new ArchitectureRule(id, label(requirement), requirement,
                    text.substring("**Reason:**".length()).strip()));
                id = null;
                requirement = null;
            }
        }
        return List.copyOf(rules);
    }

    @Override
    public String asPrompt() {
        return id + ": MUST " + requirement + " (Reason: " + reason + ")";
    }

    /** A short label for terminal output: the requirement up to its first clause break. */
    private static String label(String requirement) {
        int cut = requirement.length();
        for (String mark : List.of("; ", ". ", ", and ")) {
            int at = requirement.indexOf(mark);
            if (at > 0) {
                cut = Math.min(cut, at);
            }
        }
        return requirement.substring(0, Math.min(cut, 72)).strip();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
