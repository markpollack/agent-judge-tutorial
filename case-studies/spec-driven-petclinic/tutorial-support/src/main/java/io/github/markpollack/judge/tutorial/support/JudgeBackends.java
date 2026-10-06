package io.github.markpollack.judge.tutorial.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import io.github.markpollack.agents.claude.ClaudeAgentModel;
import io.github.markpollack.agents.claude.ClaudeAgentOptions;
import io.github.markpollack.agents.client.AgentClient;
import io.github.markpollack.judge.agentclient.AgentClientEvalModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.markpollack.judge.ai.model.EvalMessage;
import io.github.markpollack.judge.ai.model.EvalModelResponse;
import io.github.markpollack.judge.ai.model.EvalModel;

/**
 * The one place the tutorial decides where a judgment oracle's answer comes from.
 *
 * <p>Two backends, one architecture. Both are a {@link EvalModel} handed to a configured generated Judge or Jury.
 * The requirement roster and response parser stay the same; the backend supplies the native answer.
 *
 * <ul>
 * <li><b>live</b> is {@link AgentClientEvalModel} over an {@link AgentClient}. The agent can
 * read files, run commands and search the workspace before answering.</li>
 * <li><b>recorded</b> replays a captured answer so continuous integration is deterministic and
 * free. It is not a different judge; it is the same judge with its model pinned.</li>
 * </ul>
 *
 * <p>Selected by {@code AGENT_JUDGE_TUTORIAL_AGENT=live}. Recorded is the default so model replay needs no credentials. Prepare Maven
 * dependencies before running the tutorial offline.
 *
 * <p>There is deliberately no Spring AI, OpenAI, or Anthropic client anywhere in this
 * repository. Every live generated evaluation reaches its backend through AgentClient, which is what
 * lets a judge investigate rather than only opine.
 */
public final class JudgeBackends {

    /** See {@link RecordedEvalModel} for why this is SLF4J. */
    private static final Logger log = LoggerFactory.getLogger(JudgeBackends.class);

    /** Set to {@code live} to judge with a real agent. */
    public static final String MODE = "AGENT_JUDGE_TUTORIAL_AGENT";

    private JudgeBackends() {
    }

    /** True when this run judges with a real agent rather than a recorded answer. */
    public static boolean live() {
        return "live".equalsIgnoreCase(System.getenv(MODE));
    }

    /** Human-readable description of which backend a run will use, and why. */
    public static String describe() {
        return live()
            ? "live, AgentClient over Claude"
            : "recorded, replaying a captured answer\n         (set " + MODE + "=live for a real run)";
    }

    /**
     * A judging agent scoped to one workspace.
     *
     * <p>The working directory is the workspace under evaluation, so the agent's file and
     * command tools land inside the subject rather than in the tutorial's own checkout.
     */
    public static AgentClient judgingAgent(Path workspace, Duration timeout) {
        // Absolute, always. A relative working directory silently resolves against the
        // tutorial's own checkout, and the agent then sees four copies of PetClinic and
        // has to guess which one is the subject. The smoke check caught exactly that.
        Path scope = workspace.toAbsolutePath().normalize();
        ClaudeAgentModel model = ClaudeAgentModel.builder()
            .workingDirectory(scope)
            .timeout(timeout)
            .build();

        // The working directory that actually reaches the agent comes from the
        // CLIENT's default options, not the model's. DefaultAgentClient resolves
        // explicit request > goal > client defaultOptions > process cwd, and the
        // model only sees whatever that produced. Setting it on the model alone
        // leaves the agent in the tutorial's own checkout, where it finds four
        // copies of PetClinic and cannot tell which one is the subject.
        return AgentClient.builder(model)
            .defaultOptions(ClaudeAgentOptions.builder()
                .workingDirectory(scope.toString())
                .timeout(timeout)
                .build())
            .build();
    }

    /**
     * Wrap a backend so it prints the whole exchange — the prompt out, the answer back.
     *
     * <p>This is why the six-criterion slice exists. Six requirements produce a prompt and a reply
     * that fit on a screen, so the audience can read <em>exactly</em> what the judge asked and
     * <em>exactly</em> what came back, rather than being told about it. Fifty-two of anything can
     * only be summarised.
     *
     * <p>Deliberately opt-in and deliberately not used by the larger modules: the same call on the
     * full use case would print several hundred lines and teach nothing the six do not.
     */
    public static EvalModel showing(EvalModel backend) {
        return observing(backend, request -> log.info("---------- what the judge asked ----------\n{}",
            request.messages().stream().map(EvalMessage::content).collect(java.util.stream.Collectors.joining("\n"))),
            response -> log.info("---------- what came back ----------\n{}",
                response.hasAnswer() ? response.text().strip() : "(nothing)"));
    }

    private static EvalModel observing(EvalModel backend,
            java.util.function.Consumer<io.github.markpollack.judge.ai.model.EvalModelRequest> before,
            java.util.function.Consumer<EvalModelResponse> after) {
        return new EvalModel() {
            @Override public java.util.Set<io.github.markpollack.judge.ai.model.GeneratedInput> supportedInputs() {
                return backend.supportedInputs();
            }
            @Override public void validateRequest(io.github.markpollack.judge.ai.model.EvalModelRequest request) {
                backend.validateRequest(request);
            }
            @Override public EvalModelResponse generate(io.github.markpollack.judge.ai.model.EvalModelRequest request) {
                before.accept(request);
                var response = backend.generate(request);
                after.accept(response);
                return response;
            }
            @Override public io.github.markpollack.judge.execution.NativeExecution<EvalModelResponse> execute(
                    io.github.markpollack.judge.ai.model.EvalModelRequest request) {
                before.accept(request);
                var execution = backend.execute(request);
                after.accept(execution.answer());
                return execution;
            }
        };
    }

    /** The live backend: a real agent, reachable only through AgentClient. */
    public static EvalModel liveBackend(Path workspace, Duration timeout) {
        return new AgentClientEvalModel(judgingAgent(workspace, timeout));
    }

    /**
     * The backend for one named recording, at the tutorial's standard timeout.
     *
     * <p>The native generated Judge/Jury builders take a configured {@link EvalModel} — deliberately, because
     * where a judge's answers come from is the caller's business and not the library's. This is
     * the tutorial being that caller. It is the seam the promotion created, and it is one line.
     */
    public static EvalModel forRecording(Path workspace, String recording) {
        return backendFor(workspace, Duration.ofMinutes(20), recording);
    }

    /**
     * The backend a module should use for one named recording.
     *
     * <p>Recorded unless the run is live. Live runs are expensive enough that naming a single
     * recording in {@code AGENT_JUDGE_TUTORIAL_CAPTURE} means <em>refresh that one</em>: the
     * other recordings replay rather than being re-earned at twenty minutes apiece. With no
     * capture named, a live run is live throughout, which is what a demo wants.
     */
    public static EvalModel backendFor(Path workspace, Duration timeout, String recording) {
        if (!live()) {
            log.info("backend: recorded — set {}=live to run a real agent instead", MODE);
            return new RecordedEvalModel(recording);
        }
        String capture = System.getenv("AGENT_JUDGE_TUTORIAL_CAPTURE");
        if (capture != null && !capture.equals(recording)) {
            return new RecordedEvalModel(recording);
        }
        return capturing(liveBackend(workspace, timeout), recording);
    }

    /**
     * The live backend, capturing what the agent said into a recording file.
     *
     * <p>Set {@code AGENT_JUDGE_TUTORIAL_CAPTURE=<name>} alongside {@code =live} to refresh
     * the recording CI replays. The captured text is verbatim, so a recording is evidence of
     * what a real run produced rather than something written to make a module pass.
     */
    public static EvalModel capturing(EvalModel backend, String recording) {
        String capture = System.getenv("AGENT_JUDGE_TUTORIAL_CAPTURE");
        if (capture == null || !capture.equals(recording)) {
            return backend;
        }
        return observing(backend, request -> { }, response -> {
            if (!response.hasAnswer()) return;
            Path file = Path.of("tutorial-support/src/main/resources/recordings",
                recording + ".txt");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, "Captured " + java.time.LocalDate.now()
                    + " from a live AgentClient run. Verbatim agent output follows the blank line.\n\n"
                    + response.text().strip() + "\n");
                System.out.println("  [captured] " + file);
            }
            catch (IOException e) {
                throw new UncheckedIOException("Could not write recording", e);
            }
        });
    }
}
