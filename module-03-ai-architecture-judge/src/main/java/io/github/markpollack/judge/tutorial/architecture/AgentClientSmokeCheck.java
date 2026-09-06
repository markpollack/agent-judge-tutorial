/*
 * Step 2.0: is the AgentClient judging path actually wired up?
 *
 * Not a tutorial module. A one-question check that the backend modules 03, 04,
 * 06 and 07 depend on can be constructed and can answer, before any judge is
 * built on top of it.
 *
 * Run:  AGENT_JUDGE_TUTORIAL_AGENT=live ./mvnw exec:java -pl module-03-ai-architecture-judge \
 *         -Dexec.mainClass=io.github.markpollack.judge.tutorial.architecture.AgentClientSmokeCheck
 *
 * From inside a Claude Code session, run it through ~/scripts/claude-run.sh so the
 * agent process escapes the parent session's process tree.
 */
package io.github.markpollack.judge.tutorial.architecture;

import java.nio.file.Path;
import java.time.Duration;

import io.github.markpollack.judge.ai.model.JudgeModel;
import io.github.markpollack.judge.ai.model.JudgeModelRequest;
import io.github.markpollack.judge.ai.model.JudgeModelResponse;
import io.github.markpollack.judge.tutorial.build.PetClinic;

public class AgentClientSmokeCheck {

    public static void main(String[] args) {
        System.out.println("=== Step 2.0: AgentClient judging path ===\n");
        System.out.println("Backend: " + JudgeBackends.describe() + "\n");

        if (!JudgeBackends.live()) {
            System.out.println("Nothing to smoke test in recorded mode.");
            System.out.println("Set " + JudgeBackends.MODE + "=live to exercise the real path.");
            return;
        }

        Path workspace = PetClinic.candidateWorkspace();
        JudgeModel backend = JudgeBackends.liveBackend(workspace, Duration.ofMinutes(5));

        // One question with a checkable answer, asked the way a judge asks it:
        // through JudgeModelRequest, so the whole production path is exercised.
        String question = """
            Look at src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java
            in this workspace.

            Reply with exactly one line: the number of methods declared in that interface,
            then a space, then the name of the last method declared.
            """;

        System.out.println("Asking the agent a question with a checkable answer...\n");
        long started = System.currentTimeMillis();
        JudgeModelResponse response = backend.generate(JudgeModelRequest.user(question));
        long seconds = (System.currentTimeMillis() - started) / 1000;

        System.out.println("  answer     " + response.text().strip());
        System.out.println("  model      " + response.model());
        System.out.println("  metadata   " + response.metadata());
        System.out.println("  elapsed    " + seconds + "s");

        // AgentClientJudgeModel records whether the agent run itself succeeded.
        // A judge must treat this as ERROR rather than parsing a failed run's text.
        Object successful = response.metadata() == null ? null : response.metadata().get("successful");
        System.out.println("\n  agent run successful: " + successful);
        if (!Boolean.TRUE.equals(successful)) {
            System.out.println("  The run did not succeed. A judge must return ERROR here,");
            System.out.println("  not parse this text into a verdict.");
        }
    }
}
