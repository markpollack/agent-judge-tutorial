package io.github.markpollack.judge.tutorial.architecture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import io.github.markpollack.agents.claude.ClaudeAgentModel;
import io.github.markpollack.agents.claude.ClaudeAgentOptions;
import io.github.markpollack.agents.client.AgentClient;
import io.github.markpollack.judge.agentclient.AgentClientJudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModel;

/**
 * The one place the tutorial decides where a judgment oracle's answer comes from.
 *
 * <p>Two backends, one architecture. Both are a {@link JudgeModel} handed to the same
 * {@code ModelBackedJudge}, so the prompt, the classifier, and the {@code Judgment} are
 * identical either way. Only the source of the text changes.
 *
 * <ul>
 * <li><b>live</b> is {@link AgentClientJudgeModel} over an {@link AgentClient}. The agent can
 * read files, run commands and search the workspace before answering.</li>
 * <li><b>recorded</b> replays a captured answer so continuous integration is deterministic and
 * free. It is not a different judge; it is the same judge with its model pinned.</li>
 * </ul>
 *
 * <p>Selected by {@code AGENT_JUDGE_TUTORIAL_AGENT=live}. Recorded is the default so a fresh
 * clone runs offline with no credentials.
 *
 * <p>There is deliberately no Spring AI, OpenAI, or Anthropic client anywhere in this
 * repository. Every model-backed judge reaches its backend through AgentClient, which is what
 * lets a judge investigate rather than only opine.
 */
public final class JudgeBackends {

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

    /** The live backend: a real agent, reachable only through AgentClient. */
    public static JudgeModel liveBackend(Path workspace, Duration timeout) {
        return new AgentClientJudgeModel(judgingAgent(workspace, timeout));
    }

    /**
     * The live backend, capturing what the agent said into a recording file.
     *
     * <p>Set {@code AGENT_JUDGE_TUTORIAL_CAPTURE=<name>} alongside {@code =live} to refresh
     * the recording CI replays. The captured text is verbatim, so a recording is evidence of
     * what a real run produced rather than something written to make a module pass.
     */
    public static JudgeModel capturing(JudgeModel backend, String recording) {
        String capture = System.getenv("AGENT_JUDGE_TUTORIAL_CAPTURE");
        if (capture == null || !capture.equals(recording)) {
            return backend;
        }
        return request -> {
            var response = backend.generate(request);
            Path file = Path.of("module-03-ai-architecture-judge/src/main/resources/recordings",
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
            return response;
        };
    }
}
