/*
 * Centralized utilities for JBang integration tests.
 * Adapted from acp-java-tutorial integration testing framework.
 *
 * Key features:
 * - Credential-free deterministic requiredOutput validation by default
 * - Optional AI behavioral validation when explicitly enabled
 * - Uses mvn exec:java to run modules
 * - All modules run locally, no external agents needed
 */

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.zeroturnaround.exec.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static java.lang.System.*;

public class IntegrationTestUtils {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExampleInfo(
        String moduleId,
        String displayName,
        int timeoutSec,
        String[] requiredEnv,
        String expectedBehavior,
        boolean requiresPackage,
        String[] requiredOutput
    ) {
        public boolean requiresPackage() {
            return requiresPackage;
        }
        public String[] requiredOutput() {
            return requiredOutput != null ? requiredOutput : new String[0];
        }
    }

    public static ExampleInfo loadConfig(String moduleId) throws Exception {
        Path configPath = Path.of("configs", moduleId + ".json");
        if (!Files.exists(configPath)) {
            throw new RuntimeException("Config not found: " + configPath);
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(configPath.toFile(), ExampleInfo.class);
    }

    public static void verifyEnvironment(ExampleInfo cfg) {
        if (cfg.requiredEnv() != null) {
            for (String envVar : cfg.requiredEnv()) {
                if (getenv(envVar) == null) {
                    err.println("Missing required environment variable: " + envVar);
                    exit(1);
                }
            }
        }
    }

    public static Path findRepoRoot() {
        Path current = Path.of(".").toAbsolutePath().normalize();
        if (current.endsWith("integration-testing")) {
            return current.getParent();
        }
        while (current != null) {
            Path pom = current.resolve("pom.xml");
            if (Files.exists(pom)) {
                try {
                    String content = Files.readString(pom);
                    if (content.contains("<modules>")) {
                        return current;
                    }
                } catch (Exception e) {
                    // Continue searching
                }
            }
            current = current.getParent();
        }
        throw new RuntimeException("Could not find repository root with parent pom.xml");
    }

    public static void buildModule(ExampleInfo cfg) throws Exception {
        Path repoRoot = findRepoRoot();
        String goal = cfg.requiresPackage() ? "package" : "compile";
        out.println("Building " + cfg.moduleId() + " (" + goal + ")...");

        ProcessResult result = new ProcessExecutor()
            .command(mavenCommand(goal, "-DskipTests", "-pl", cfg.moduleId(), "-q"))
            .directory(repoRoot.toFile())
            .timeout(300, TimeUnit.SECONDS)
            .redirectOutput(out)
            .redirectError(err)
            .execute();
        if (result.getExitValue() != 0) {
            throw new RuntimeException("Build failed for " + cfg.moduleId());
        }
    }

    public static Path createLogFile(String moduleId) throws Exception {
        Path logDir = Path.of("logs");
        Files.createDirectories(logDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        return logDir.resolve(moduleId + "-" + timestamp + ".log");
    }

    public static ProcessResult runModule(ExampleInfo cfg, Path logFile) throws Exception {
        Path repoRoot = findRepoRoot();
        return new ProcessExecutor()
            .command(mavenCommand("exec:java", "-pl", cfg.moduleId(), "-q"))
            .directory(repoRoot.toFile())
            .timeout(cfg.timeoutSec(), TimeUnit.SECONDS)
            .redirectOutput(Files.newOutputStream(logFile))
            .redirectErrorStream(true)
            .execute();
    }

    private static List<String> mavenCommand(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("./mvnw");
        String repository = getenv("AGENT_JUDGE_TUTORIAL_MAVEN_REPO");
        if (repository != null && !repository.isBlank()) {
            command.add("-Dmaven.repo.local=" + repository);
        }
        String agentJudgeVersion = getenv("AGENT_JUDGE_TUTORIAL_AGENT_JUDGE_VERSION");
        if (agentJudgeVersion != null && !agentJudgeVersion.isBlank()) {
            command.add("-Dagent-judge.version=" + agentJudgeVersion);
        }
        command.addAll(Arrays.asList(arguments));
        return command;
    }

    public static void displayOutputPreview(String output) {
        out.println("Output Preview (first 50 lines):");
        out.println("---");
        String[] lines = output.split("\n");
        for (int i = 0; i < Math.min(50, lines.length); i++) {
            out.println(lines[i]);
        }
        if (lines.length > 50) {
            out.println("... (" + (lines.length - 50) + " more lines)");
        }
        out.println("---");
    }

    public static List<String> checkRequiredOutput(String output, String[] requiredOutput) {
        List<String> missing = new java.util.ArrayList<>();
        for (String required : requiredOutput) {
            if (!output.contains(required)) {
                missing.add(required);
            }
        }
        return missing;
    }

    public static void runIntegrationTest(String moduleId) throws Exception {
        out.println("Integration Test: " + moduleId);
        out.println("=".repeat(60));

        ExampleInfo cfg = loadConfig(moduleId);
        out.println(cfg.displayName());
        out.println("Timeout: " + cfg.timeoutSec() + "s");

        // Verify environment
        out.println("\nVerifying environment...");
        verifyEnvironment(cfg);
        out.println("Environment OK");

        // Build module
        out.println("\nBuilding module...");
        buildModule(cfg);
        out.println("Build complete");

        // Create log file
        Path logFile = createLogFile(moduleId);

        // Run module
        out.println("\nRunning " + moduleId + "...");
        ProcessResult result = runModule(cfg, logFile);
        int exitCode = result.getExitValue();

        // Read output
        String output = Files.readString(logFile);

        // Display output preview
        out.println("\nModule Output:");
        displayOutputPreview(output);
        out.println("Full log: " + logFile.toAbsolutePath());

        // Check exit code
        if (exitCode != 0) {
            err.println("\nFAILED: Module exited with code: " + exitCode);
            exit(exitCode);
        }

        // Deterministic output check (hard gate before AI validation)
        List<String> missingOutput = checkRequiredOutput(output, cfg.requiredOutput());
        if (!missingOutput.isEmpty()) {
            err.println("\nFAILED: Required output missing from " + cfg.displayName());
            err.println("  The following required strings were NOT found in the output:");
            for (String missing : missingOutput) {
                err.println("    - \"" + missing + "\"");
            }
            err.println("\n  Fix the module code or update requiredOutput in configs/" + cfg.moduleId() + ".json");
            exit(1);
        }
        if (cfg.requiredOutput().length > 0) {
            out.println("\nAll " + cfg.requiredOutput().length + " required output strings found");
        }

        if (!Boolean.parseBoolean(getenv("AGENT_JUDGE_TUTORIAL_AI_VALIDATE"))) {
            out.println("\nAI validation skipped (set AGENT_JUDGE_TUTORIAL_AI_VALIDATE=true to enable)");
            out.println("\n" + "=".repeat(60));
            out.println("PASSED: " + cfg.displayName());
            return;
        }

        // Optional AI validation
        out.println("\nRunning optional AI validation...");
        AIValidator.ValidationResult validation = AIValidator.validate(
            output,
            cfg.expectedBehavior(),
            cfg.displayName()
        );

        out.println("  Success:    " + validation.success());
        out.println("  Confidence: " + String.format("%.2f", validation.confidence()));
        out.println("  Reasoning:  " + validation.reasoning());

        if (validation.issues() != null && !validation.issues().isEmpty()) {
            out.println("  Issues:");
            for (String issue : validation.issues()) {
                out.println("    - " + issue);
            }
        }

        out.println("\n" + "=".repeat(60));
        if (validation.success()) {
            out.println("PASSED: " + cfg.displayName());
        } else {
            err.println("FAILED: " + cfg.displayName());
            exit(1);
        }
    }
}
