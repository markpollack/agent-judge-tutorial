package io.github.markpollack.judge.tutorial.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.ai.model.EvalModelRequest;
import io.github.markpollack.judge.ai.model.EvalModelResponse;

/**
 * Test-only backend. Replays an answer a real agent gave, so continuous integration is
 * deterministic, free, and offline.
 *
 * <p>The configured RFC2119/EARS Judge or Jury supplies an
 * {@link EvalModelRequest} for its actual requirement or selected roster.
 * This backend returns the recorded answer in an {@link EvalModelResponse};
 * the producer still parses that answer and retains the resulting judgments.
 * Replay verifies caller wiring and result semantics, not evaluator accuracy.
 *
 * <p>Each recording is a file under {@code src/main/resources/recordings/} captured from a live
 * run. The header comment in each records when and against what it was captured.
 *
 * <p>A missing recording is an {@code ERROR}, never a default pass. A stand-in that answers
 * whatever it has not seen is a judge that cannot fail, which is worse than no judge.
 */
public final class RecordedEvalModel implements EvalModel {

    /**
     * SLF4J, because that is what this repository and the library already use — {@code slf4j-simple}
     * is a compile-scope dependency of the root POM, which this case study inherits, so INFO goes to
     * stderr with no configuration.
     *
     * <p>This exists because a passing JUnit test prints nothing, so a judge that replays in two
     * seconds is indistinguishable from a judge that did not run. These lines say what was asked,
     * what answered, and where that answer came from.
     */
    private static final Logger log = LoggerFactory.getLogger(RecordedEvalModel.class);

    /**
     * What this backend says when it cannot answer, and the ERROR message the operator sees.
     *
     * <p>It is a whole sentence rather than a marker because the judge no longer translates it. A
     * judge is handed a model and cannot know why one failed to answer — only the backend knows
     * that it was a missing recording rather than a timeout or a crashed agent. So the backend
     * carries its own explanation in the response text, alongside {@code completed=false}, and the
     * judge reports it verbatim.
     *
     * <p>This is what keeps DD-8 working through a library boundary: the failure of an instrument
     * must never be rendered as a finding about the subject.
     */
    public static final String NO_RECORDING = "No recording to replay; capture one with "
        + "AGENT_JUDGE_TUTORIAL_AGENT=live AGENT_JUDGE_TUTORIAL_CAPTURE=<name>";

    private final String recording;

    public RecordedEvalModel(String recording) {
        this.recording = recording;
    }

    /**
     * Replay one verbatim answer line from an archived roster response, retaining the complete
     * original response and provenance. This is an extraction for teaching a single Judge;
     * it is not a new single-requirement investigation. Roster demos replay the full recording.
     */
    public static EvalModel singleRequirement(String recording, String requirementId) {
        RecordedEvalModel archive = new RecordedEvalModel(recording);
        return request -> {
            EvalModelResponse original = archive.generate(request);
            if (!original.completed()) {
                return original;
            }
            var lines = original.text().lines()
                .filter(line -> line.startsWith(requirementId + ":"))
                .toList();
            if (lines.size() != 1) {
                throw new IllegalStateException("Expected one archived answer for " + requirementId
                    + " in " + recording + "; found " + lines.size());
            }
            var facts = new java.util.LinkedHashMap<>(original.metadata());
            facts.put("archivedRosterResponse", original.text());
            facts.put("selectedRequirement", requirementId);
            facts.put("replayMode", "verbatim single-line extraction; not fresh inference");
            return new EvalModelResponse(lines.getFirst(), original.model(), original.usage(), facts, true);
        };
    }

    @Override
    public EvalModelResponse generate(EvalModelRequest request) {
        String path = "/recordings/" + recording + ".txt";
        try (InputStream in = RecordedEvalModel.class.getResourceAsStream(path)) {
            if (in == null) {
                log.warn("no recording named '{}'", recording);
                return new EvalModelResponse(NO_RECORDING, "recorded", null, Map.of("recording", recording), false);
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // Strip the provenance header; everything after the first blank line is
            // exactly what the agent returned.
            int body = text.indexOf("\n\n");
            String answer = body < 0 ? text : text.substring(body + 2).strip();

            String provenance = body < 0 ? "no provenance header" : text.substring(0, body).strip();
            log.info("replaying recording '{}' — {}", recording, provenance);
            log.info("prompt {} chars in · answer {} lines back",
                request.messages().stream().mapToInt(m -> m.content().length()).sum(),
                answer.lines().count());

            return new EvalModelResponse(answer, "recorded", null, Map.of("recording", recording, "provenance", provenance), true);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read recording " + path, e);
        }
    }
}
