/*
 * Module 08: ModelBackedJudge
 *
 * Composed AI judge: prompt template + model backend + classifier.
 * No subclassing needed. Each component is independently swappable.
 *
 * This demo uses a stub JudgeModel to show the composition pattern
 * without requiring an API key. In production, use SpringAiJudgeModel
 * (from agent-judge-llm) or AgentClientJudgeModel (from agent-judge-agent-client).
 *
 * Run: ./mvnw exec:java -pl module-08-model-backed-judge
 */
package io.github.markpollack.judge.tutorial.module08;

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
        System.out.println("=== Module 08: ModelBackedJudge Demo ===\n");

        // --- Step 1: Define a prompt template ---
        // Templates use {{variable}} placeholders extracted from JudgmentContext.
        // Available: {{goal}}, {{output}}, {{workspace}}, {{status}}, {{metadata.*}}
        JudgePromptTemplate template = JudgePromptTemplate.fromString(
            "goal-completion",
            """
            You are evaluating whether an AI agent accomplished its goal.

            Goal: {{goal}}
            Agent output: {{output}}
            Execution status: {{status}}

            Did the agent accomplish the goal? Answer exactly PASS or FAIL.
            """);
        System.out.println("Template: " + template.name());

        // --- Step 2: Create a JudgeModel ---
        // In production: SpringAiJudgeModel or AgentClientJudgeModel.
        // Here we use a stub that simulates model responses.
        JudgeModel stubModel = request -> {
            String prompt = request.messages().getFirst().content();
            // Simulate: if output mentions the goal keyword, model says PASS
            boolean relevant = prompt.contains("HelloController") && prompt.contains("SUCCESS");
            String response = relevant ? "PASS" : "FAIL";
            System.out.println("  [Stub model] Received prompt (" + prompt.length() + " chars)");
            System.out.println("  [Stub model] Responding: " + response);
            return new JudgeModelResponse(response, "stub-model", null, null);
        };

        // --- Step 3: Build the judge ---
        // Three parts composed via builder: template + model + classifier
        ModelBackedJudge judge = ModelBackedJudge.builder()
            .name("goal-completion")
            .description("Evaluates whether the agent accomplished its goal")
            .promptTemplate(template)
            .model(stubModel)
            .judgmentClassifier(JudgmentClassifiers.passFail("PASS", "FAIL"))
            .build();

        System.out.println("\nJudge: " + judge.metadata().name());
        System.out.println("Type:  " + judge.metadata().type());

        // --- Step 4: Evaluate with a passing context ---
        System.out.println("\n--- Passing case ---");
        JudgmentContext passContext = JudgmentContext.builder()
            .goal("Add a HelloController class")
            .workspace(Path.of("test-workspace"))
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .agentOutput("Created HelloController.java with hello() method")
            .build();

        Judgment passResult = judge.judge(passContext);
        System.out.println("  Status:    " + passResult.status());
        System.out.println("  Stored score (optional): " + passResult.score());
        System.out.println("  Reasoning: " + passResult.reasoning());

        // --- Step 5: Evaluate with a failing context ---
        System.out.println("\n--- Failing case ---");
        JudgmentContext failContext = JudgmentContext.builder()
            .goal("Add a REST endpoint")
            .workspace(Path.of("test-workspace"))
            .status(ExecutionStatus.FAILED)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(30))
            .agentOutput("Error: compilation failed")
            .build();

        Judgment failResult = judge.judge(failContext);
        System.out.println("  Status:    " + failResult.status());
        System.out.println("  Reasoning: " + failResult.reasoning());

        System.out.println("\nDone.");
    }
}
