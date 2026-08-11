/*
 * Module 10: LangChain4j Evaluation
 *
 * Run: ./mvnw exec:java -pl module-10-langchain4j-evaluation
 */
package io.github.markpollack.judge.tutorial.module10;

import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.langchain4j.LangChain4jEvaluator;
import io.github.markpollack.judge.result.Judgment;

public class LangChain4jEvaluationDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 10: LangChain4j Evaluation Demo ===\n");

        Judge addressesTopic = (JudgmentContext context) -> {
            String output = context.agentOutput().orElse("").toLowerCase();
            boolean relevant = output.contains("spring boot");
            return Judgment.verdict(relevant)
                .reasoning(relevant ? "Answer addresses Spring Boot" : "Answer misses the requested topic")
                .build();
        };

        Judgment judgment = LangChain4jEvaluator.evaluate(
            "What is Spring Boot?",
            goal -> Result.<String>builder()
                .content("Spring Boot simplifies creating production-ready Spring applications.")
                .finishReason(FinishReason.STOP)
                .tokenUsage(new TokenUsage(50, 30))
                .build(),
            addressesTopic);

        System.out.println("Status:    " + judgment.status());
        System.out.println("Reasoning: " + judgment.reasoning());
        System.out.println("\nDone.");
    }
}
