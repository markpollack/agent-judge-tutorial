/*
 * Module 07: Inside the judgment oracle
 *
 * Module 01 used a model-backed judge and did not open it. This is the
 * inside, and there is less to it than the name suggests: three independent
 * parts, composed, no subclassing.
 *
 *     JudgePromptTemplate  renders the evaluation input from the context
 *     JudgeModel           calls a backend
 *     JudgmentClassifier   turns the reply into a Judgment
 *
 * Each is swappable on its own. Swapping the model is the difference between
 * running offline and calling a real one; nothing else moves.
 *
 * Run: ./mvnw exec:java -pl module-07-model-backed-judge
 */
package io.github.markpollack.judge.tutorial.module07;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.ai.JudgmentClassifiers;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModelResponse;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Judgment;

public class ModelBackedJudgeDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 07: Inside the judgment oracle ===\n");

        // --- Part 1: the prompt template ---
        // Ordinary text with {{variable}} placeholders drawn from the context:
        // {{goal}}, {{output}}, {{workspace}}, {{status}}, {{metadata.*}}.
        JudgePromptTemplate template = JudgePromptTemplate.fromString(
            "intent-satisfied",
            """
            An agent was asked to do something. Decide whether it did.

            Goal:   {{goal}}
            Output: {{output}}
            Status: {{status}}

            Answer exactly SATISFIED or NOT_SATISFIED.
            """);

        JudgmentContext context = JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(Path.of("test-workspace"))
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofMinutes(2))
            .agentOutput("Added ReportController with a report(from, to) method")
            .build();

        System.out.println("--- 1. The prompt, rendered from the context ---\n");
        template.render(context).lines().forEach(line -> System.out.println("  | " + line));

        // --- Part 2: the model ---
        // A fixture, so the module runs with no credentials. It replays two
        // replies and refuses anything else: a stand-in that answers whatever
        // it has not seen is a judge that cannot fail.
        //
        // In production this line becomes SpringAiJudgeModel (agent-judge-llm)
        // or AgentClientJudgeModel (agent-judge-agent-client). Nothing else in
        // this file changes.
        JudgeModel fixture = request -> {
            String prompt = request.messages().getFirst().content();
            String reply;
            if (prompt.contains("Added ReportController")) {
                reply = "SATISFIED";
            }
            else if (prompt.contains("compilation failed")) {
                reply = "NOT_SATISFIED";
            }
            else {
                reply = "I am not sure";
            }
            System.out.println("\n  [fixture] replying: " + reply);
            return new JudgeModelResponse(reply, "fixture", null, null);
        };

        // --- Part 3: the classifier ---
        // The mapping from the model's vocabulary to the judgment vocabulary.
        // It is explicit on purpose: a classifier that guessed would be the
        // place where a judge quietly starts inventing verdicts.
        ModelBackedJudge judge = ModelBackedJudge.builder()
            .name("intent-satisfied")
            .description("Did the agent do what it was asked?")
            .promptTemplate(template)
            .model(fixture)
            .judgmentClassifier(JudgmentClassifiers.passFail("SATISFIED", "NOT_SATISFIED"))
            .build();

        System.out.println("\n--- 2. A reply the classifier recognises ---");
        show(judge.judge(context));

        System.out.println("\n--- 3. The other one ---");
        show(judge.judge(JudgmentContext.builder()
            .goal("Expose a sales report endpoint")
            .workspace(Path.of("test-workspace"))
            .status(ExecutionStatus.FAILED)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(30))
            .agentOutput("Error: compilation failed")
            .build()));

        // --- What happens when the model says something else entirely ---
        System.out.println("\n--- 4. A reply the classifier does not recognise ---");
        show(judge.judge(JudgmentContext.builder()
            .goal("Something the fixture has no reply for")
            .workspace(Path.of("test-workspace"))
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(1))
            .agentOutput("...")
            .build()));

        para("""
            ABSTAIN, with the raw text kept in metadata - not a guess, and not a
            default PASS. An unparseable reply means the judge reached no
            finding, which is exactly the case module 09 makes a jury decide
            about.

            That is the whole judge. A template, a model, and a mapping. The
            model is the only part that costs anything, and the only part you
            have to replace to make it real.
            """);

        System.out.println("Done.");
    }

    private static void show(Judgment judgment) {
        System.out.println("  status:    " + judgment.status());
        System.out.println("  label:     " + judgment.label());
        System.out.println("  score:     " + judgment.score());
        System.out.println("  reasoning: " + judgment.reasoning());
    }

    private static void para(String text) {
        System.out.println();
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
