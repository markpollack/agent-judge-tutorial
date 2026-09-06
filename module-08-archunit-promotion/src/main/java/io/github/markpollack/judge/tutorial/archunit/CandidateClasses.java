package io.github.markpollack.judge.tutorial.archunit;

import java.nio.file.Files;
import java.nio.file.Path;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * The compiled candidate, as ArchUnit sees it.
 *
 * <p>This is the first real difference between module 07 and module 08. The AI judge read source
 * files; ArchUnit reads bytecode, which means the candidate has to have been compiled first. That
 * is not a detail — it is what a deterministic architectural check costs, and it is also why the
 * check can be exact about things a reader cannot be: which types a field actually holds after
 * generics erase, which packages a class actually reaches through a call it never names.
 */
public final class CandidateClasses {

    private CandidateClasses() {
    }

    /**
     * Import the candidate's main classes.
     *
     * <p>Test classes are excluded. A rule about production structure that a test class can
     * violate is a rule about tests, and mixing the two is how an architecture check ends up
     * reporting failures nobody intends to fix.
     *
     * @throws IllegalStateException if the candidate has not been built, because an ArchUnit run
     * over an empty import is a pass over nothing
     */
    public static JavaClasses of(Path workspace) {
        Path classes = workspace.resolve("target/classes");
        if (!Files.isDirectory(classes)) {
            throw new IllegalStateException("Candidate is not built: " + classes
                + " does not exist. ArchUnit reads bytecode, so run the build first.");
        }
        JavaClasses imported = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPath(classes);

        // A rule set that matched nothing reports that everything is fine. State the size.
        if (imported.isEmpty()) {
            throw new IllegalStateException("Imported no classes from " + classes);
        }
        return imported;
    }
}
