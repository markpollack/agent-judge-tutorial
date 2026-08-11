/*
 * Module 09: Koog Evaluation
 *
 * Run: ./mvnw exec:java -pl module-09-koog-evaluation
 */
package io.github.markpollack.judge.tutorial.module09;

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
        System.out.println("=== Module 09: Koog Evaluation Demo ===\n");

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
