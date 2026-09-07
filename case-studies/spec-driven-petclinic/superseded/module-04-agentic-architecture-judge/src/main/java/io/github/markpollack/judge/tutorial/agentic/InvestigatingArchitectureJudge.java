package io.github.markpollack.judge.tutorial.agentic;

import java.nio.file.Path;
import java.time.Duration;

import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.tutorial.architecture.ArchitecturalConformanceJudge;

/**
 * The same architectural-conformance judge as module 03, with its evidence taken away.
 *
 * <p>Module 03 handed the judge two files and a diff, which means somebody had already decided
 * what the relevant convention was. That decision is most of the work, and on a codebase you do
 * not know, you cannot make it.
 *
 * <p>Here the judge gets a workspace and tools. It has to find the convention itself, say how much
 * code it looked at to conclude that, and name what departs from it. The prompt asks for a
 * denominator on purpose: a claim about "the dominant pattern" drawn from one file is not a
 * finding.
 *
 * <p>What changes is the prompt. The classifier, the backend selection, the {@code Judgment} and
 * the {@code Check}s are all the module 03 machinery, because this is one concept at a later
 * stage rather than a second judge.
 */
public final class InvestigatingArchitectureJudge {

    private InvestigatingArchitectureJudge() {
    }

    private static final JudgePromptTemplate TEMPLATE = JudgePromptTemplate.fromString(
        "investigating-architecture",
        """
        You are reviewing one change to a Java codebase you have not seen before.
        You are in its root. Use the tools you have: read files, grep, and git.

        The agent that made the change was given this task:
        {{goal}}

        Work in this order.

        1. Find what changed. `git diff` will not help, this is not a git working tree,
           so look for the feature the task describes and the code around it.
        2. Establish the convention the change should have followed, by reading the
           code that was already there. Do not assume; go and look.
        3. Decide whether the change follows it.

        Judge conformance to the established pattern, not whether that pattern is good.
        Faithful duplication of an existing shape is conformance. Ignore formatting and
        imports: this project enforces those with spring-javaformat and Checkstyle.

        Answer in exactly this form and nothing else, using plain names with no markup:

          PATTERN: <the dominant convention you found, in one line>
          POPULATION: <what you actually examined, with counts>
          criterion-name: PASS|FAIL - <one sentence of evidence, citing a file>
          criterion-name: PASS|FAIL - <one sentence of evidence, citing a file>
          VERDICT: PASS|FAIL
          SUMMARY: <one sentence>
        """);

    /** Build the investigating stage of the architecture judge. */
    public static ModelBackedJudge create(Path workspace, String recording) {
        return ArchitecturalConformanceJudge.judge(
            "architectural-conformance",
            "Find the convention, then judge the change against it",
            TEMPLATE, workspace, recording, Duration.ofMinutes(8),
            // Discovery names its own criteria, so it cannot be held to module 03's roster.
            ArchitecturalConformanceJudge.discoveryClassifier());
    }
}
