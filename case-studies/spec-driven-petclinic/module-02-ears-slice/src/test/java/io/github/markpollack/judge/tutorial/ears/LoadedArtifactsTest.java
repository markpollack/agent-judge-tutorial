package io.github.markpollack.judge.tutorial.ears;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Prints actual loaded JAR identities; optionally checks the preparation manifest and cache. */
class LoadedArtifactsTest {
    @Test
    void showAndVerifyLoadedLibraries() throws Exception {
        String repository = System.getProperty("maven.repo.local");
        String manifestPath = System.getProperty("eval.artifacts");
        List<String> manifest = manifestPath == null ? List.of() : Files.readAllLines(Path.of(manifestPath));
        for (String name : List.of(
                "io.github.markpollack.judge.Judge",
                "io.github.markpollack.judge.ai.requirements.EarsJudge",
                "io.github.markpollack.judge.agentclient.AgentClientEvalModel",
                "io.github.markpollack.judge.serialization.VerdictCodec",
                "io.github.markpollack.judge.assertj.Assertions",
                "io.github.markpollack.judge.assertions.RequirementAssertions",
                "com.fasterxml.jackson.databind.ObjectMapper",
                "com.fasterxml.jackson.annotation.JsonProperty")) {
            Path jar = Path.of(Class.forName(name).getProtectionDomain().getCodeSource().getLocation().toURI());
            assertTrue(jar.toString().endsWith(".jar"), "Expected installed JAR: " + jar);
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
            System.out.println("LOADED " + name + " " + sha256 + " " + jar);
            if (repository != null) {
                assertTrue(jar.startsWith(Path.of(repository).toAbsolutePath()), "Unexpected cache: " + jar);
            }
            if (name.startsWith("io.github.markpollack.judge.") && manifestPath != null) {
                assertTrue(manifest.contains(sha256 + "  " + jar), "Bytes differ from preparation: " + jar);
            }
        }
    }
}
