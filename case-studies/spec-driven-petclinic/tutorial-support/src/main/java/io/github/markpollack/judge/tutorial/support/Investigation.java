package io.github.markpollack.judge.tutorial.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a failed requirement actually means, and whether it can happen.
 *
 * <h2>A failure is an address, not yet a finding</h2>
 *
 * <p>{@link Rfc2119Judge} answers whether each constraint holds. When one does not, what it
 * produces is a <em>lead</em>: an identifier, a location, and a hypothesis. That is genuinely
 * useful and it is genuinely not a finding. Nobody can act on
 * "RULE-4 FAIL at StaffFallbackService.java:247" without first answering two questions the judge
 * was never asked:
 *
 * <pre>
 *   consequence    what goes wrong if this stays as it is?
 *   reachability   can that actually be reached in this implementation?
 * </pre>
 *
 * <p>This is the second tier. It reads one failed check and answers exactly those two questions.
 *
 * <h2>Why this is asked as a separate question, by a separate call</h2>
 *
 * <p>Measured, not assumed. Across 65 requirements in two runs, every file-and-line citation the
 * judge produced was correct and every aggregate claim in its surrounding prose was wrong. The
 * reliable half of a judge's output is the address. Establishing what an address <em>means</em>
 * needs a different question, a fresh reading, and the freedom to disagree.
 *
 * <h2>The framing constraint is load-bearing</h2>
 *
 * <p>The investigation is asked to <b>establish the consequence and whether it is reachable</b>.
 * It is never asked to <em>verify the finding</em>. Those sound similar and are not. Asking an
 * agent to verify a finding produces an agent that agrees, and a second tier that only ever
 * confirms is an expensive echo of the first.
 *
 * <p>So {@link Outcome#DE_ESCALATED} and {@link Reachability#NOT_REACHABLE} are first-class
 * results, named in the prompt as valuable rather than tolerated. An investigation that comes back
 * saying the lead was overstated has done its job, not failed at it.
 *
 * <h2>No severity, no rank, no score</h2>
 *
 * <p>There is no severity field and no numeric anything, for the reason in the design: a rating is
 * not actionable and cannot be re-derived. What is stored is the consequence in words, the
 * reachability argument, and the citations behind both — all of which a reader can go and check.
 *
 * @param requirementId the requirement whose failure was investigated, such as {@code RULE-4}
 * @param outcome what the investigation concluded relative to the lead it started from
 * @param consequence one sentence: what goes wrong if this is left alone
 * @param reachability whether that consequence can be reached in this implementation
 * @param reachabilityArgument why — including any narrowing of the original hypothesis
 * @param citations {@code file:line} references supporting the above, possibly empty
 */
public record Investigation(String requirementId, Outcome outcome, String consequence,
        Reachability reachability, String reachabilityArgument, List<String> citations) {

    /**
     * What the investigation concluded, relative to the lead it was given.
     *
     * <p>Named relative to the lead on purpose. The interesting information is not "is this bad"
     * but "did reading past the cited line change what we believed".
     */
    public enum Outcome {

        /** The lead was accurate as stated. */
        CONFIRMED,

        /** The consequence is worse, wider, or lands somewhere other than the cited line. */
        ESCALATED,

        /** The lead was overstated. A valuable result, not a failure of the investigation. */
        DE_ESCALATED,

        /** Neither the consequence nor its reachability could be settled from the code available. */
        CANNOT_ESTABLISH
    }

    /** Whether the consequence can actually be reached in this implementation. */
    public enum Reachability {

        /** There is a path. The argument says which. */
        REACHABLE,

        /** The feared consequence cannot be reached. The lead may still be a real rule violation. */
        NOT_REACHABLE,

        /** Not settled either way from the code available. */
        UNDETERMINED
    }

    public Investigation {
        citations = List.copyOf(citations);
    }

    /** {@code StaffFallbackService.java:248}, and the bare {@code :258} form a citation may use. */
    private static final Pattern LOCATION =
        Pattern.compile("[A-Za-z0-9_/.]*[A-Za-z0-9_]+\\.(?:java|xml|sql|html|yml|properties):\\d+");

    /**
     * Parse one investigation out of an agent's reply.
     *
     * <p>Strict about the fields it needs and forgiving about everything else. A reply that omits
     * the consequence or the reachability has not answered the question that was asked, and this
     * returns empty rather than inventing the missing half — the same discipline the judges use
     * for an unanswered requirement. Blaming the subject for a malformed reply would be a lie
     * about where the problem is.
     */
    public static java.util.Optional<Investigation> parse(String requirementId, String text) {
        if (text == null || text.isBlank()) {
            return java.util.Optional.empty();
        }
        Outcome outcome = null;
        Reachability reachability = null;
        String consequence = null;
        List<String> argument = new ArrayList<>();
        List<String> citations = new ArrayList<>();
        boolean inArgument = false;

        for (String raw : structuredPart(text).lines().toList()) {
            String line = raw.strip();
            String upper = line.toUpperCase();
            if (upper.startsWith("OUTCOME:")) {
                outcome = outcomeOf(value(line));
                inArgument = false;
            }
            else if (upper.startsWith("CONSEQUENCE:")) {
                consequence = value(line);
                inArgument = false;
            }
            else if (upper.startsWith("REACHABILITY:")) {
                reachability = reachabilityOf(value(line));
                inArgument = false;
            }
            else if (upper.startsWith("ARGUMENT:")) {
                // The value may be empty, with the argument starting on the next line. Tracking
                // this with a flag rather than "have we collected anything yet" is the difference
                // between keeping a multi-paragraph argument and silently dropping all of it.
                inArgument = true;
                argument.add(value(line));
            }
            else if (upper.startsWith("CITATION:")) {
                String citation = value(line);
                if (!citation.isEmpty() && !citations.contains(citation)) {
                    citations.add(citation);
                }
                inArgument = false;
            }
            else if (inArgument && !upper.startsWith("OBSERVATION")) {
                // A continuation of a multi-line ARGUMENT. Reachability arguments are the one
                // field that genuinely needs more than a sentence, because they have to name a
                // path rather than assert one. Blank lines are kept: they are the only structure
                // the argument has, and the first paragraph is what a terminal has room for.
                argument.add(line);
            }
        }

        if (outcome == null || reachability == null || consequence == null || consequence.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new Investigation(requirementId, outcome, consequence,
            reachability, paragraphs(argument), citations));
    }

    /** Every {@code file:line} mentioned anywhere in the citations, deduplicated, in order. */
    public List<String> locations() {
        List<String> found = new ArrayList<>();
        for (String citation : citations) {
            Matcher matcher = LOCATION.matcher(citation);
            while (matcher.find()) {
                if (!found.contains(matcher.group())) {
                    found.add(matcher.group());
                }
            }
        }
        return List.copyOf(found);
    }

    /**
     * The argument's opening paragraph — what a terminal has room for.
     *
     * <p>The full argument is kept intact on the record and in the committed recording. A
     * reachability argument earns its length by naming paths rather than asserting them, and
     * truncating it in storage would throw away the part that makes it checkable. Choosing how
     * much to display is a presentation decision and belongs at the point of display.
     */
    public String reachabilitySummary() {
        int block = reachabilityArgument.indexOf("\n\n");
        return block < 0 ? reachabilityArgument : reachabilityArgument.substring(0, block).strip();
    }

    /** True when the investigation changed what the lead claimed, in either direction. */
    public boolean changedTheLead() {
        return outcome == Outcome.ESCALATED || outcome == Outcome.DE_ESCALATED;
    }

    /** The field names the reply is asked to use, longest-lived contract in this class. */
    private static final List<String> FIELDS =
        List.of("OUTCOME:", "CONSEQUENCE:", "REACHABILITY:", "ARGUMENT:", "CITATION:");

    /**
     * Everything from the first field marker onwards.
     *
     * <p>An agent that can run commands narrates while it works, and that narration arrives ahead
     * of the structured reply — sometimes running straight into it on the same line, with no
     * newline between "cleaning up the temporary files" and {@code OUTCOME:}. Requiring the first
     * field to begin a line rejected a perfectly good investigation for a cosmetic reason.
     *
     * <p>So the preamble is dropped rather than parsed. This deliberately does not re-split later
     * fields: only the boundary between narration and reply is ambiguous, and splitting on every
     * occurrence would cut an argument in half the moment it mentioned one of these words.
     */
    private static String structuredPart(String text) {
        int start = -1;
        for (String field : FIELDS) {
            int at = text.toUpperCase().indexOf(field);
            if (at >= 0 && (start < 0 || at < start)) {
                start = at;
            }
        }
        return start <= 0 ? text : text.substring(start);
    }

    /** Join collected argument lines, collapsing runs of blank lines to a single break. */
    private static String paragraphs(List<String> lines) {
        StringBuilder text = new StringBuilder();
        boolean pendingBreak = false;
        for (String line : lines) {
            if (line.isBlank()) {
                pendingBreak = text.length() > 0;
                continue;
            }
            if (pendingBreak) {
                text.append("\n\n");
                pendingBreak = false;
            }
            else if (text.length() > 0) {
                text.append('\n');
            }
            text.append(line);
        }
        return text.toString().strip();
    }

    private static String value(String line) {
        int colon = line.indexOf(':');
        return colon < 0 ? "" : line.substring(colon + 1).strip();
    }

    private static Outcome outcomeOf(String text) {
        String upper = text.toUpperCase();
        for (Outcome candidate : Outcome.values()) {
            if (upper.startsWith(candidate.name())) {
                return candidate;
            }
        }
        return null;
    }

    private static Reachability reachabilityOf(String text) {
        String upper = text.toUpperCase();
        // NOT_REACHABLE before REACHABLE: the longer name is a suffix trap for the shorter.
        if (upper.startsWith("NOT_REACHABLE") || upper.startsWith("NOT REACHABLE")) {
            return Reachability.NOT_REACHABLE;
        }
        if (upper.startsWith("REACHABLE")) {
            return Reachability.REACHABLE;
        }
        if (upper.startsWith("UNDETERMINED")) {
            return Reachability.UNDETERMINED;
        }
        return null;
    }
}
