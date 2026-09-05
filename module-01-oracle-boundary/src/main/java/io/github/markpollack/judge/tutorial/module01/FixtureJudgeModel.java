package io.github.markpollack.judge.tutorial.module01;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModelRequest;
import io.github.markpollack.judge.ai.model.JudgeModelResponse;

/**
 * A {@link JudgeModel} that calls nothing, so the tutorial runs with no credentials and
 * no network.
 *
 * <p><strong>What is a fixture here and what is not.</strong> The two review texts below
 * are written by hand. They are what we claim a competent reviewer would say about each
 * controller, in the shape a real model returns. They are not a transcript of a live
 * call, and a fixture can never tell you that a judge is <em>right</em> — only that the
 * wiring around it is.
 *
 * <p>Everything else on the path is real: the prompt is rendered from the actual
 * {@code JudgmentContext}, the classifier parses free text into a {@code Judgment} with
 * {@code Check}s, and the judge is an ordinary {@code ModelBackedJudge}. Swapping this
 * class for {@code SpringAiJudgeModel} or {@code AgentClientJudgeModel} is a one-line
 * change and nothing else moves.
 *
 * <p>An unrecorded subject returns {@link #NO_RECORDING} rather than a default. A stand-in
 * that quietly approves whatever it has not seen is worse than no judge at all, because
 * it is a judge that cannot fail.
 */
final class FixtureJudgeModel implements JudgeModel {

    /** Marker the classifier turns into an ERROR, never into a verdict. */
    static final String NO_RECORDING = "NO_RECORDING";

    private static final Map<String, String> REVIEWS = reviews();

    @Override
    public JudgeModelResponse generate(JudgeModelRequest request) {
        String prompt = request.messages().getFirst().content();
        for (Map.Entry<String, String> review : REVIEWS.entrySet()) {
            if (prompt.contains("class " + review.getKey())) {
                return new JudgeModelResponse(review.getValue(), "fixture", null, null);
            }
        }
        return new JudgeModelResponse(NO_RECORDING, "fixture", null, null);
    }

    private static Map<String, String> reviews() {
        Map<String, String> reviews = new LinkedHashMap<>();

        reviews.put("ReportController", """
            layering: FAIL - opens its own JDBC connection, not a service
            serialization: FAIL - builds JSON by hand; the codebase returns records
            error-handling: FAIL - catches Exception and returns an empty result
            resource-handling: FAIL - connection, statement and result set never closed
            sql-construction: FAIL - concatenates request parameters into the SQL
            naming: PASS - class name and package match the codebase
            VERDICT: FAIL
            SUMMARY: It compiles and is named correctly, but it reimplements data access, serialization \
            and error handling inside a controller, which is neither how this codebase works nor the \
            simplest thing that would have worked.
            """);

        reviews.put("HelloController", """
            layering: PASS - holds no logic; delegates to GreetingService
            serialization: PASS - returns a Greeting record
            error-handling: PASS - nothing is caught and discarded
            resource-handling: PASS - opens no resource
            naming: PASS - class name and package match the codebase
            VERDICT: PASS
            SUMMARY: HelloController is the idiom the rest of the codebase follows.
            """);

        return reviews;
    }
}
