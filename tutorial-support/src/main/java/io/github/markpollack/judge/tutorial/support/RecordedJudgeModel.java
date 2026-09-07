package io.github.markpollack.judge.tutorial.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModelRequest;
import io.github.markpollack.judge.ai.model.JudgeModelResponse;

/**
 * Test-only backend. Replays an answer a real agent gave, so continuous integration is
 * deterministic, free, and offline.
 *
 * <p><b>This is not a different judge.</b> It is the same {@code ModelBackedJudge} with its
 * model pinned. The prompt is still rendered from the real {@code JudgmentContext}, the
 * classifier still parses the text, and the resulting {@code Judgment} is built the same way.
 * What a recorded run verifies is the wiring and the semantics, never that the judge is right.
 *
 * <p>Each recording is a file under {@code src/main/resources/recordings/} captured from a live
 * run. The header comment in each records when and against what it was captured.
 *
 * <p>A missing recording is an {@code ERROR}, never a default pass. A stand-in that answers
 * whatever it has not seen is a judge that cannot fail, which is worse than no judge.
 */
public final class RecordedJudgeModel implements JudgeModel {

    /** Marker the classifier turns into ERROR rather than a verdict. */
    public static final String NO_RECORDING = "NO_RECORDING";

    private final String recording;

    public RecordedJudgeModel(String recording) {
        this.recording = recording;
    }

    @Override
    public JudgeModelResponse generate(JudgeModelRequest request) {
        String path = "/recordings/" + recording + ".txt";
        try (InputStream in = RecordedJudgeModel.class.getResourceAsStream(path)) {
            if (in == null) {
                return new JudgeModelResponse(NO_RECORDING, "recorded", null, Map.of("successful", false));
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // Strip the provenance header; everything after the first blank line is
            // exactly what the agent returned.
            int body = text.indexOf("\n\n");
            return new JudgeModelResponse(body < 0 ? text : text.substring(body + 2).strip(),
                "recorded", null, Map.of("successful", true));
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read recording " + path, e);
        }
    }
}
