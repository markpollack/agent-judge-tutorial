/*
 * Module 01: Does it build?
 *
 * An agent was given a written specification and produced a working Spring
 * application. It says it is done. Should you merge?
 *
 * The first part of that question is not a matter of opinion, and you already
 * own the instrument that answers it. javac decides whether it compiles, JUnit
 * decides whether the tests pass, and Maven's exit code carries both.
 *
 * Agent Judge does not improve on any of that, and does not try to. What it adds
 * is a shape: an engineering fact becomes a Judgment that other kinds of
 * evidence can sit beside. Use the least interpretive instrument that can
 * reliably answer the question -- here, that is a build.
 *
 * Run: ./mvnw exec:java -pl module-01-build
 */
package io.github.markpollack.judge.tutorial.build;

import java.nio.file.Path;

import io.github.markpollack.judge.exec.BuildSuccessJudge;
import io.github.markpollack.judge.result.Judgment;
import io.github.markpollack.judge.tutorial.support.Candidate;

public class BuildDemo {

    public static void main(String[] args) {
        System.out.println("=== Module 01: Does it build? ===\n");
        System.out.println("The agent was asked:");
        System.out.println("  \"" + Candidate.GOAL + "\"");
        System.out.println("\nIt produced 166 Java files and says it is done.\n");

        Path workspace = Candidate.workspace();
        System.out.println("Subject: " + workspace);
        System.out.println("Command: ./mvnw test\n");
        System.out.println("Running the real build. This takes about a minute.\n");

        Judgment judgment = BuildSuccessJudge.maven("test").judge(Candidate.contextFor(workspace));

        System.out.println("  build   " + judgment.status());
        System.out.println();

        para("""
            javac and JUnit decided that, not Agent Judge. The exit code carried
            their answer and a Judgment gave it a shape.

            What one PASS establishes: the code compiles and the tests that exist
            pass. What it does not establish is anything about whether the agent
            built what was asked for -- the build has no opinion on that, and
            neither do the tests, because the same run that wrote the code wrote
            most of them.

            Somebody did write down what was asked for, before any code existed.
            Module 02 reads it.
            """);
        System.out.println("Done.");
    }

    private static void para(String text) {
        text.stripTrailing().lines()
            .forEach(line -> System.out.println(line.isBlank() ? "" : "      " + line));
        System.out.println();
    }
}
