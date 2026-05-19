/*
 * Module 04: Simple Jury
 *
 * Multi-judge aggregation with SimpleJury. Three named judges with
 * different weights, aggregated by majority voting.
 *
 * Run: ./mvnw exec:java -pl module-04-simple-jury
 */
package io.github.markpollack.judge.tutorial.module04;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.context.ExecutionStatus;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.fs.FileContentJudge;
import io.github.markpollack.judge.fs.FileExistsJudge;
import io.github.markpollack.judge.jury.MajorityVotingStrategy;
import io.github.markpollack.judge.jury.SimpleJury;
import io.github.markpollack.judge.jury.Verdict;
import io.github.markpollack.judge.jury.WeightedAverageStrategy;

public class SimpleJuryDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 04: Simple Jury Demo ===\n");

        Path workspace = Path.of("test-workspace");
        String controllerPath = "src/main/java/com/example/HelloController.java";

        JudgmentContext context = JudgmentContext.builder()
            .goal("Add a HelloController class")
            .workspace(workspace)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now())
            .executionTime(Duration.ofSeconds(5))
            .build();

        // Three named judges with weights
        Judge fileExists = Judges.named(
            new FileExistsJudge(controllerPath),
            "file-exists", "Controller file created");

        Judge hasMethod = Judges.named(
            new FileContentJudge(controllerPath, "hello",
                FileContentJudge.MatchMode.CONTAINS),
            "has-method", "Contains hello method");

        Judge hasPom = Judges.named(
            new FileExistsJudge("pom.xml"),
            "has-pom", "Maven project file exists");

        // --- Majority voting ---
        System.out.println("--- Majority Voting ---");
        SimpleJury majorityJury = SimpleJury.builder()
            .judge(fileExists, 1.0)
            .judge(hasMethod, 1.0)
            .judge(hasPom, 1.0)
            .votingStrategy(new MajorityVotingStrategy())
            .parallel(true)
            .build();

        Verdict majorityVerdict = majorityJury.vote(context);

        System.out.println("Overall: " + majorityVerdict.aggregated().status());
        System.out.println("Reason:  " + majorityVerdict.aggregated().reasoning());
        System.out.println();

        majorityVerdict.individualByName().forEach((name, judgment) ->
            System.out.printf("  %-15s %s  %s%n",
                name, judgment.status(), judgment.reasoning()));

        System.out.println("  Weights: " + majorityVerdict.weights());

        // --- Weighted average ---
        System.out.println("\n--- Weighted Average ---");
        SimpleJury weightedJury = SimpleJury.builder()
            .judge(fileExists, 1.0)
            .judge(hasMethod, 2.0)   // method check weighted 2x
            .judge(hasPom, 1.0)
            .votingStrategy(new WeightedAverageStrategy())
            .build();

        Verdict weightedVerdict = weightedJury.vote(context);

        System.out.println("Overall: " + weightedVerdict.aggregated().status());
        System.out.println("Score:   " + weightedVerdict.aggregated().score());

        System.out.println("\nDone.");
    }
}
