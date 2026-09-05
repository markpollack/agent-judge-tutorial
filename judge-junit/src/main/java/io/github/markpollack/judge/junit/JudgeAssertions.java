package io.github.markpollack.judge.junit;

import java.util.List;
import java.util.Map;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.jury.Jury;
import io.github.markpollack.judge.jury.Verdict;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

import org.opentest4j.AssertionFailedError;

/**
 * Turns an Agent Judge outcome into a JUnit assertion failure that says why.
 *
 * <p>This is the whole bridge. It decides nothing: it runs no policy, applies no
 * threshold, and adds no state. It exists because {@code assertTrue(judgment.pass())}
 * throws away every piece of evidence the judge just produced, and leaves you with
 * "expected true, was false" for a criterion that had a reason.
 *
 * <h2>Assert the status, not {@code pass()}</h2>
 *
 * <p>{@link Judgment#pass()} is {@code false} for {@code FAIL}, {@code ERROR} and
 * {@code ABSTAIN} alike, so a test written on {@code pass()} cannot tell a judge that
 * rejected the subject from a judge that never ran. Every method here compares
 * {@link JudgmentStatus} exactly, and a failure message names which of the four states
 * actually occurred.
 *
 * <p>That distinction is the reason {@link #assertFail} exists rather than
 * {@code assertFalse(judgment.pass())}. A judge that errored is not a caught bug.
 *
 * <h2>Lambdas and overload resolution</h2>
 *
 * <p>{@code Judge} and {@code Jury} are both functional interfaces taking a
 * {@link JudgmentContext}, so an implicitly typed lambda passed to
 * {@code assertPass(judge, context)} is ambiguous. Name the judge first — which is
 * worth doing anyway, because an unnamed judge has no identity in a verdict:
 *
 * <pre>{@code
 * assertPass(Judges.named(ctx -> ..., "has-tests"), context);
 * }</pre>
 */
public final class JudgeAssertions {

    private JudgeAssertions() {
    }

    // ---------------------------------------------------------------- Judgment

    /** Assert that a judgment passed. */
    public static void assertPass(Judgment judgment) {
        assertStatus(JudgmentStatus.PASS, judgment);
    }

    /**
     * Assert that a judgment failed.
     *
     * <p>{@code ERROR} and {@code ABSTAIN} do not satisfy this. A judge that could not
     * complete has not rejected anything.
     */
    public static void assertFail(Judgment judgment) {
        assertStatus(JudgmentStatus.FAIL, judgment);
    }

    /** Assert a judgment's exact status. */
    public static void assertStatus(JudgmentStatus expected, Judgment judgment) {
        if (judgment.status() != expected) {
            throw new AssertionFailedError(describe("judgment", expected, judgment),
                expected, judgment.status());
        }
    }

    /** Run the judge, assert it passed, and return the judgment for further inspection. */
    public static Judgment assertPass(Judge judge, JudgmentContext context) {
        return assertStatus(JudgmentStatus.PASS, judge, context);
    }

    /** Run the judge, assert it failed, and return the judgment for further inspection. */
    public static Judgment assertFail(Judge judge, JudgmentContext context) {
        return assertStatus(JudgmentStatus.FAIL, judge, context);
    }

    /** Run the judge, assert its exact status, and return the judgment. */
    public static Judgment assertStatus(JudgmentStatus expected, Judge judge, JudgmentContext context) {
        Judgment judgment = judge.judge(context);
        assertStatus(expected, judgment);
        return judgment;
    }

    // ------------------------------------------------------------------ Verdict

    /** Assert that a verdict's aggregate passed. */
    public static void assertPass(Verdict verdict) {
        assertStatus(JudgmentStatus.PASS, verdict);
    }

    /** Assert that a verdict's aggregate failed. */
    public static void assertFail(Verdict verdict) {
        assertStatus(JudgmentStatus.FAIL, verdict);
    }

    /** Assert a verdict's exact aggregate status, reporting every member by name. */
    public static void assertStatus(JudgmentStatus expected, Verdict verdict) {
        Judgment aggregated = verdict.aggregated();
        if (aggregated.status() != expected) {
            throw new AssertionFailedError(describeVerdict(expected, verdict),
                expected, aggregated.status());
        }
    }

    /** Run the jury, assert its aggregate passed, and return the verdict. */
    public static Verdict assertPass(Jury jury, JudgmentContext context) {
        return assertStatus(JudgmentStatus.PASS, jury, context);
    }

    /** Run the jury, assert its aggregate failed, and return the verdict. */
    public static Verdict assertFail(Jury jury, JudgmentContext context) {
        return assertStatus(JudgmentStatus.FAIL, jury, context);
    }

    /** Run the jury, assert its exact aggregate status, and return the verdict. */
    public static Verdict assertStatus(JudgmentStatus expected, Jury jury, JudgmentContext context) {
        Verdict verdict = jury.vote(context);
        assertStatus(expected, verdict);
        return verdict;
    }

    // ------------------------------------------------------------------ Message

    private static String describe(String subject, JudgmentStatus expected, Judgment judgment) {
        StringBuilder message = new StringBuilder();
        message.append("Expected ").append(subject).append(' ').append(expected)
            .append(" but was ").append(judgment.status());
        appendMeaning(message, judgment.status());
        message.append("\n  reasoning: ").append(judgment.reasoning());
        appendChecks(message, judgment.checks(), "  ");
        return message.toString();
    }

    private static String describeVerdict(JudgmentStatus expected, Verdict verdict) {
        Judgment aggregated = verdict.aggregated();
        StringBuilder message = new StringBuilder();
        message.append("Expected verdict ").append(expected)
            .append(" but was ").append(aggregated.status());
        appendMeaning(message, aggregated.status());
        message.append("\n  reasoning: ").append(aggregated.reasoning());

        List<Judgment> members = verdict.individual();
        Map<String, Judgment> named = verdict.individualByName();
        message.append("\n  ").append(members.size()).append(" judge")
            .append(members.size() == 1 ? "" : "s").append(':');
        if (!named.isEmpty()) {
            named.forEach((name, member) -> appendMember(message, name, member));
        }
        else {
            for (int i = 0; i < members.size(); i++) {
                appendMember(message, "[" + i + "]", members.get(i));
            }
        }

        appendRoster(message, aggregated);
        appendChecks(message, aggregated.checks(), "  ");
        return message.toString();
    }

    private static void appendMember(StringBuilder message, String name, Judgment member) {
        message.append("\n    ").append(pad(member.status().toString(), 8)).append(name);
        if (member.status() != JudgmentStatus.PASS) {
            message.append("\n        ").append(member.reasoning());
            appendChecks(message, member.checks(), "        ");
        }
    }

    /**
     * Report the roster only when it does not match, because a jury that quietly scored
     * with fewer judges than it lists is the failure that otherwise reads as a pass.
     */
    private static void appendRoster(StringBuilder message, Judgment aggregated) {
        Object block = aggregated.metadata().get(Judgment.AGGREGATION_KEY);
        if (!(block instanceof Map<?, ?> evidence)) {
            return;
        }
        Object input = evidence.get("inputCount");
        Object eligible = evidence.get("eligibleCount");
        if (input instanceof Number configured && eligible instanceof Number counted
            && configured.intValue() != counted.intValue()) {
            message.append("\n  roster: ").append(configured.intValue())
                .append(" configured, ").append(counted.intValue())
                .append(" counted toward the aggregate");
        }
    }

    private static void appendChecks(StringBuilder message, List<Check> checks, String indent) {
        List<Check> failed = checks.stream().filter(check -> !check.passed()).toList();
        if (failed.isEmpty()) {
            return;
        }
        message.append('\n').append(indent).append(failed.size()).append(" of ")
            .append(checks.size()).append(" checks failed:");
        for (Check check : failed) {
            message.append('\n').append(indent).append("  - ").append(check.name());
            if (!check.message().isBlank()) {
                message.append(": ").append(check.message());
            }
        }
    }

    private static void appendMeaning(StringBuilder message, JudgmentStatus actual) {
        switch (actual) {
            case ERROR -> message.append("  (the judge did not complete, so it rejected nothing)");
            case ABSTAIN -> message.append("  (the judge cast no vote)");
            default -> {
            }
        }
    }

    private static String pad(String value, int width) {
        return value.length() >= width ? value + " " : value + " ".repeat(width - value.length());
    }
}
