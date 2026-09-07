package io.github.markpollack.judge.tutorial.archunit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tngtech.archunit.core.domain.JavaClasses;

import io.github.markpollack.judge.jury.AllMustPassStrategy;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.result.JudgmentStatus;

import io.github.markpollack.judge.tutorial.archunit.PromotedRules.NamedRule;

/**
 * A judge with no model in it.
 *
 * <p>This is the end of the arc that started with a build. Module 03 needed a model because
 * nothing else could read a codebase for a convention. Module 07 needed one because thirteen
 * architectural rules are written in English. This needs nothing: the two rules that were
 * promoted are decidable from bytecode, so the judge is a loop over ArchUnit rules and the
 * answer is the same every time it runs.
 *
 * <p>The {@code Judgment} it returns is the same type module 07 returns, which is the point.
 * A definition of done does not care whether a requirement was settled by a model, a parser, or
 * a compiler; it cares that every requirement was settled. This one just happens to be free.
 *
 * <h2>The denominator, again</h2>
 *
 * <p>An ArchUnit rule that matches no classes passes. Two of them pass twice as convincingly.
 * {@link CandidateClasses} refuses to import an unbuilt workspace for that reason, and the rule
 * count is reported next to the verdict, because "0 of 0 rules violated" and "all rules hold"
 * render identically and mean opposite things.
 */
public final class ArchUnitJudge {

    private ArchUnitJudge() {
    }

    /** Run the promoted rules over a built candidate. */
    public static Judgment judge(Path workspace, List<NamedRule> rules) {
        if (rules.isEmpty()) {
            return Judgment.error("No rules to run; a check over an empty rule set is not a pass");
        }
        JavaClasses classes = CandidateClasses.of(workspace);

        List<Judgment> perRule = new ArrayList<>();
        List<Check> checks = new ArrayList<>();
        for (NamedRule named : rules) {
            try {
                named.rule().check(classes);
                perRule.add(Judgment.pass(named.ruleId()));
                checks.add(Check.pass(named.ruleId(), named.description() + ": holds"));
            }
            catch (AssertionError violation) {
                List<String> lines = violation.getMessage().lines().skip(1)
                    .map(String::strip).filter(line -> !line.isEmpty()).toList();
                perRule.add(Judgment.fail(named.ruleId()));
                checks.add(Check.fail(named.ruleId(), named.description() + ": "
                    + lines.size() + " violation(s), first at " + firstLocation(lines)));
            }
        }

        Judgment rolled = new AllMustPassStrategy().aggregate(perRule, Map.of());
        return Judgment.verdict(rolled.status() == JudgmentStatus.PASS)
            .reasoning("%d of %d promoted rules hold over %d classes"
                .formatted(checks.stream().filter(Check::passed).count(), rules.size(), classes.size()))
            .checks(checks)
            .metadata("rulesRun", rules.size())
            .metadata("classesExamined", classes.size())
            .build();
    }

    /** Every violation names a file and line; report the first so the message points somewhere. */
    private static String firstLocation(List<String> lines) {
        return lines.stream().findFirst()
            .map(line -> {
                int open = line.lastIndexOf('(');
                return open < 0 ? line : line.substring(open + 1).replace(")", "");
            })
            .orElse("an unreported location");
    }

    /** Every violation line, for a module that wants to print them. */
    public static List<String> violations(Path workspace, NamedRule named) {
        try {
            named.rule().check(CandidateClasses.of(workspace));
            return List.of();
        }
        catch (AssertionError violation) {
            return violation.getMessage().lines().skip(1)
                .map(String::strip).filter(line -> !line.isEmpty()).toList();
        }
    }
}
