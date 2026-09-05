/*
 * Module 11: The same bar again, through a different framework
 *
 * LangChain4j hands back a Result<T>. LangChain4jEvaluator adapts it into a
 * JudgmentContext and applies an ordinary Judge — the same shape as module 10,
 * with a different framework on the far side.
 *
 * Two frameworks, one evaluation layer, and a judge that does not know or care
 * which one produced the output it is reading.
 *
 * Run: ./mvnw exec:java -pl module-11-langchain4j-evaluation
 */
package io.github.markpollack.judge.tutorial.module11;

import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.langchain4j.LangChain4jEvaluator;
import io.github.markpollack.judge.result.Judgment;

public class LangChain4jEvaluationDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 11: Evaluating a LangChain4j result ===\n");

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
