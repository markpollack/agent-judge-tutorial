package io.github.markpollack.judge.tutorial.spec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The rubric, read from the specification rather than invented.
 *
 * <p>This is the case a judge author rarely gets and should take when offered. Module 03 had to
 * decide what the criteria were, and choosing them badly made the judge unstable. Here the
 * criteria already exist, numbered, with stable identifiers, because somebody wrote them down
 * before the code was built. The judge does not get to pick.
 *
 * <p>It also fixes the denominator. A specification with 52 acceptance criteria has 52, and a
 * judge that answers five of them has not evaluated the specification: it has sampled it. Report
 * the number you were asked and the number you answered, always.
 */
public record SpecCriteria(String id, String title, String requirement) {

    /** {@code ### UC6-AC5: Permit adjacent future appointments} */
    private static final Pattern HEADING = Pattern.compile("^### (UC\\d+-AC\\d+): (.+)$");

    /**
     * Read every acceptance criterion out of a use case's {@code criteria.md}.
     *
     * <p>The requirement text is the first non-empty prose line after the heading that is not a
     * {@code **Covers:**} traceability marker, which is the shape every criterion in this
     * specification uses.
     */
    public static List<SpecCriteria> from(Path criteriaFile) {
        List<SpecCriteria> criteria = new ArrayList<>();
        String id = null;
        String title = null;

        for (String line : read(criteriaFile).lines().toList()) {
            Matcher heading = HEADING.matcher(line.strip());
            if (heading.matches()) {
                id = heading.group(1);
                title = heading.group(2).strip();
                continue;
            }
            if (id == null) {
                continue;
            }
            String text = line.strip();
            if (text.isEmpty() || text.startsWith("**Covers:**") || text.startsWith("#")) {
                continue;
            }
            criteria.add(new SpecCriteria(id, title, text));
            id = null;
            title = null;
        }
        return List.copyOf(criteria);
    }

    /** The criterion as the judge is asked to assess it. */
    public String asPrompt() {
        return id + ": " + requirement;
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
