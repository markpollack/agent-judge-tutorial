/*
 * Module 03: does the change fit?
 *
 * The build passed. Coverage held. Neither of them can tell you whether the
 * agent did what it was actually asked, because the task said "follow the
 * conventions already used in the codebase" and no exit code decides that.
 *
 * This is the first question in the tutorial with no expected value to compare
 * against. The oracle has to be produced rather than looked up, and that is the
 * only thing here that justifies a model.
 *
 * The backend is AgentClient. No provider SDK appears anywhere in this tutorial.
 *
 * Run:                    ./mvnw exec:java -pl module-03-ai-architecture-judge \
 *                           -Dexec.mainClass=...ArchitectureJudgeDemo
 * With a real agent:      AGENT_JUDGE_TUTORIAL_AGENT=live ...
 */
package io.github.markpollack.judge.tutorial.architecture;

import java.nio.file.Path;

import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class ArchitectureJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 03: Does the change fit? ===\n");
        System.out.println("The agent was asked to follow the conventions already used");
        System.out.println("in the codebase. Nothing so far has checked whether it did.\n");
        System.out.println("Backend: " + JudgeBackends.describe() + "\n");

        Path workspace = PetClinic.candidateWorkspace();
        JudgmentContext context = ArchitecturalConformanceJudge.contextFor(
            workspace,
            PetClinic.baselineWorkspace(),
            Path.of("fixtures/petclinic/city-search.patch"));

        System.out.println("Evidence supplied to the judge:");
        System.out.println("  the existing OwnerController and OwnerRepository");
        System.out.println("  the 85 line diff the agent produced");
        System.out.println("\nAsking...\n");

        long started = System.currentTimeMillis();
        Judgment judgment = ArchitecturalConformanceJudge.create(workspace).judge(context);
        long seconds = (System.currentTimeMillis() - started) / 1000;

        System.out.println("  architectural-conformance  " + judgment.status() + "   (" + seconds + "s)");
        wrap(judgment.reasoning());
        System.out.println();
        judgment.checks().forEach(check ->
            System.out.printf("    %-4s  %s%n", check.passed() ? "PASS" : "FAIL", check.name()));
        System.out.println();
        judgment.checks().forEach(check -> {
            System.out.println("  " + check.name() + ":");
            wrap(check.message());
        });

        // A count over criteria the judge actually assessed. Derived here at read
        // time from the checks, not stored: an aggregate is cheap to recompute from
        // parts, and parts cannot be recovered from an aggregate.
        long met = judgment.checks().stream().filter(check -> check.passed()).count();
        System.out.printf("%n  %d of %d criteria met%n", met, judgment.checks().size());

        para("""
            That count is a measurement, and it is the one number this judge is
            entitled to. It was computed from criteria the judge actually assessed,
            it is reproducible, and it can be traced back to the criterion that
            failed. Compare that to asking a model for "conformance: 0.85", which
            is a number with no procedure behind it and no way to say what would
            have made it 0.86.

            It reports. It does not decide. Four of five is not eighty percent of
            done, and a strong criterion must not pay for a weak one, so the gate
            stays conjunctive.

            What the count is for is movement. PASS and FAIL cannot tell one gap
            remaining from ignoring the pattern entirely, so with a Boolean alone
            you cannot tell whether a change to your agent improved anything. Four
            of five becoming five of five is a signal.
            """);

        para("""
            The judgment kept its criteria. Nothing was averaged into a score,
            because a number cannot say which criterion binds and that is the only
            part anybody acts on.

            You can check this one by eye. Open the diff and the controller it
            copies, and decide whether you agree. That is deliberate: a judgment
            oracle you cannot audit on a small case is not one you should trust on
            a large one.

            Next: this judge was handed its evidence. Module 04 stops supplying it
            and lets the judge go and look.
            """);

        System.out.println("Done.");
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
