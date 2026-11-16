/*
 * Module 07: Inside the judgment oracle
 *
 * Module 01 used a model-backed judge and did not open it. This is the
 * inside, and there is less to it than the name suggests: three independent
 * parts, composed, no subclassing.
 *
 *     JudgePromptTemplate  renders the evaluation input from the evidence
 *     EvalModel            calls a backend
 *     JudgmentClassifier   turns the reply into a Judgment
 *
 * Each is swappable on its own. Swapping the model is the difference between
 * running offline and calling a real one; nothing else moves.
 *
 * Run: ./mvnw exec:java -pl module-07-model-backed-judge
 */
package io.github.markpollack.judge.tutorial.module07;

import java.util.Map;
import java.util.function.Function;

import io.github.markpollack.judge.ai.JudgmentClassifiers;
import io.github.markpollack.judge.ai.ModelBackedJudge;
import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.ai.model.EvalModelResponse;
import io.github.markpollack.judge.ai.prompt.JudgePromptTemplate;
import io.github.markpollack.judge.judgment.Judgment;

public class ModelBackedJudgeDemo {

    /** The evidence this judge reads: what the agent was asked, what it said, how it ended. */
    record AgentRun(String goal, String output, String status) {
    }

    public static void main(String[] args) {
        System.out.println("=== Module 07: Inside the judgment oracle ===\n");

        // --- Part 1: the prompt template ---
        // Ordinary text with {{variable}} placeholders. The variables come from
        // the evidence, through a function you write: nothing is filled in that
        // you did not name.
        JudgePromptTemplate template = JudgePromptTemplate.fromString(
            "intent-satisfied",
            """
            An agent was asked to do something. Decide whether it did.

            Goal:   {{goal}}
            Output: {{output}}
            Status: {{status}}

            Answer exactly SATISFIED or NOT_SATISFIED.
            """);

        Function<AgentRun, Map<String, Object>> variables = run -> Map.of(
            "goal", run.goal(), "output", run.output(), "status", run.status());

        AgentRun succeeded = new AgentRun("Expose a sales report endpoint",
            "Added ReportController with a report(from, to) method", "SUCCESS");

        System.out.println("--- 1. The prompt, rendered from the evidence ---\n");
        template.render(variables.apply(succeeded)).lines().forEach(line -> System.out.println("  | " + line));

        // --- Part 2: the model ---
        // A fixture, so the module runs with no credentials. It replays two
        // replies and refuses anything else: a stand-in that answers whatever
        // it has not seen is a judge that cannot fail.
        //
        // In production this line becomes SpringAiEvalModel (agent-judge-llm)
        // or AgentClientEvalModel (agent-judge-agent-client). Nothing else in
        // this file changes.
        EvalModel fixture = request -> {
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
            return new EvalModelResponse(reply, "fixture", null, null);
        };

        // --- Part 3: the classifier ---
        // The mapping from the model's vocabulary to the judgment vocabulary.
        // It is explicit on purpose: a classifier that guessed would be the
        // place where a judge quietly starts inventing verdicts.
        //
        // The builder is a recipe: each .evidence(...).build() makes one ready
        // judge for one run, and judge() takes no argument.
        ModelBackedJudge.Builder<AgentRun> recipe = ModelBackedJudge.<AgentRun>builder()
            .name("intent-satisfied")
            .description("Did the agent do what it was asked?")
            .promptTemplate(template)
            .variables(variables)
            .model(fixture)
            .judgmentClassifier(JudgmentClassifiers.passFail("SATISFIED", "NOT_SATISFIED"));

        System.out.println("\n--- 2. A reply the classifier recognises ---");
        show(recipe.evidence(succeeded).build().judge());

        System.out.println("\n--- 3. The other one ---");
        show(recipe.evidence(new AgentRun("Expose a sales report endpoint",
            "Error: compilation failed", "FAILED")).build().judge());

        // --- What happens when the model says something else entirely ---
        System.out.println("\n--- 4. A reply the classifier does not recognise ---");
        show(recipe.evidence(new AgentRun("Something the fixture has no reply for",
            "...", "SUCCESS")).build().judge());

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
