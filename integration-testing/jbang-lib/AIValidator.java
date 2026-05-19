/*
 * AI Validation using Claude Code SDK.
 * Validates tutorial output by asking Claude to analyze if it demonstrates expected behavior.
 */

import io.github.markpollack.claude.agent.sdk.Query;
import io.github.markpollack.claude.agent.sdk.QueryOptions;
import com.fasterxml.jackson.databind.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static java.lang.System.*;

public class AIValidator {

    private static final String VALIDATION_MODEL = "claude-haiku-4-5-20251001";
    private static final Duration VALIDATION_TIMEOUT = Duration.ofSeconds(60);

    public record ValidationResult(
        boolean success,
        double confidence,
        String reasoning,
        List<String> issues
    ) {}

    public static ValidationResult validate(String logOutput, String expectedBehavior, String moduleName) {
        String prompt = buildValidationPrompt(logOutput, expectedBehavior, moduleName);

        try {
            QueryOptions options = QueryOptions.builder()
                .model(VALIDATION_MODEL)
                .timeout(VALIDATION_TIMEOUT)
                .maxTurns(1)
                .disallowedTools(List.of("Bash", "Write", "Edit", "Read", "Glob", "Grep"))
                .appendSystemPrompt("You are a tutorial validator. Analyze the output and respond ONLY with valid JSON.")
                .build();

            String response = Query.text(prompt, options);
            return parseResponse(response);

        } catch (Exception e) {
            err.println("AI validation error: " + e.getMessage());
            return new ValidationResult(
                false,
                0.0,
                "AI validation failed: " + e.getMessage(),
                List.of("Exception during validation: " + e.getClass().getSimpleName())
            );
        }
    }

    private static String buildValidationPrompt(String logOutput, String expectedBehavior, String moduleName) {
        String truncatedOutput = logOutput;
        if (logOutput.length() > 10000) {
            truncatedOutput = logOutput.substring(0, 5000) +
                "\n... [" + (logOutput.length() - 10000) + " chars truncated] ...\n" +
                logOutput.substring(logOutput.length() - 5000);
        }

        return """
            You are a tutorial validator for the Agent Judge Tutorial.

            ## Module: %s

            ## Expected Behavior:
            %s

            ## Actual Output:
            ```
            %s
            ```

            ## Validation Task:
            Analyze if this tutorial output demonstrates the expected behavior.

            Look for:
            1. Module header present (=== Module NN: ===)
            2. Expected judge/jury evaluation demonstrated
            3. Correct judgment statuses (PASS/FAIL) based on what was evaluated
            4. Reasoning text present for each judgment
            5. No fatal errors, stack traces, or exceptions
            6. "Done." printed at the end (clean exit)

            Be precise - verify that the described functionality was actually demonstrated in the output.
            If the expected behavior says a judge should "pass", verify the output shows PASS status.
            If scores or checks are mentioned, verify they appear.

            Respond ONLY with valid JSON (no markdown, no explanation):
            {
              "success": true or false,
              "confidence": 0.0 to 1.0,
              "reasoning": "one sentence explanation",
              "issues": ["list", "of", "problems"] or []
            }
            """.formatted(moduleName, expectedBehavior, truncatedOutput);
    }

    private static ValidationResult parseResponse(String response) {
        try {
            String jsonStr = extractJson(response);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(jsonStr, Map.class);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            double confidence = result.get("confidence") instanceof Number n ? n.doubleValue() : 0.5;
            String reasoning = result.get("reasoning") != null ? result.get("reasoning").toString() : "No reasoning provided";

            @SuppressWarnings("unchecked")
            List<String> issues = result.get("issues") instanceof List<?> list
                ? list.stream().map(Object::toString).toList()
                : List.of();

            return new ValidationResult(success, confidence, reasoning, issues);

        } catch (Exception e) {
            boolean looksSuccessful = response.toLowerCase().contains("\"success\": true") ||
                                     response.toLowerCase().contains("\"success\":true");
            return new ValidationResult(
                looksSuccessful,
                0.5,
                "Could not parse structured response: " + e.getMessage(),
                List.of("Response parsing failed")
            );
        }
    }

    private static String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
    }
}
