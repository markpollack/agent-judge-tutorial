package io.github.markpollack.judge.tutorial.archunit;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;

import io.github.markpollack.judge.tutorial.build.PetClinic;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateClassesTest {

    @Test
    void importsTheCandidatesProductionClasses() {
        JavaClasses classes = CandidateClasses.of(PetClinic.largeCandidate());
        assertTrue(classes.size() > 100, "imported only " + classes.size() + " classes");
        assertTrue(classes.stream().noneMatch(c -> c.getName().endsWith("Tests")),
            "test classes must not be imported");
    }

    @Test
    void anUnbuiltCandidateIsAnErrorNotAnEmptyPass() {
        assertThrows(IllegalStateException.class, () -> CandidateClasses.of(Path.of("/nonexistent")));
    }
}
