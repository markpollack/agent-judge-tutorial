/*
 * Module 10: The same bar, applied to a real agent framework
 *
 * Everything so far judged a workspace. An agent framework hands you its own
 * result type instead, and an evaluator is the adapter: it runs or wraps that
 * result, puts it in a JudgmentContext, and then applies an ordinary Judge.
 *
 * That is the whole bridge. The judge does not know it came from Koog, which
 * is the point - the bar you built in module 06 is the same bar here.
 *
 * The agent is a deterministic mock so the module runs with no credentials.
 * Its output is fixed; the evaluation path around it is real.
 *
 * Run: ./mvnw exec:java -pl module-10-koog-evaluation
 */
package io.github.markpollack.judge.tutorial.module10;

import ai.koog.agents.core.agent.AIAgent;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.koog.KoogEvaluator;
import io.github.markpollack.judge.result.Judgment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KoogEvaluationDemo {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("=== Module 10: Evaluating a Koog agent ===\n");

        AIAgent<String, String> agent = mock(AIAgent.class);
        when(agent.run("Explain dependency injection"))
            .thenReturn("Dependencies are supplied by an external source instead of created internally.");
        when(agent.getId()).thenReturn("docs-assistant");

        Judge describesDependencyInjection = (JudgmentContext context) -> {
            String output = context.agentOutput().orElse("").toLowerCase();
            boolean complete = output.contains("dependencies") && output.contains("external");
            return Judgment.verdict(complete)
                .reasoning(complete
                    ? "Answer describes external dependency supply"
                    : "Answer omits a key dependency-injection concept")
                .build();
        };

        Judgment judgment = KoogEvaluator.evaluate(
            agent, "Explain dependency injection", describesDependencyInjection);

        System.out.println("Agent ID:  docs-assistant");
        System.out.println("Status:    " + judgment.status());
        System.out.println("Reasoning: " + judgment.reasoning());
        System.out.println("\nDone.");
    }
}
