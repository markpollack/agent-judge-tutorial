package io.github.markpollack.judge.tutorial.module05;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.DeterministicJudge;
import io.github.markpollack.judge.judgment.Check;
import io.github.markpollack.judge.judgment.Judgment;

/**
 * A reusable deterministic judge that verifies Java package structure.
 * Extends DeterministicJudge for metadata support and granular checks.
 *
 * <p>The evidence is the workspace to inspect, bound when the judge is built.
 */
public class PackageStructureJudge extends DeterministicJudge<Path> {

    private final String expectedPackage;
    private final String expectedClass;

    public PackageStructureJudge(Path workspace, String expectedPackage, String expectedClass) {
        super(() -> workspace, "package-structure",
            String.format("Verifies %s.%s exists with correct package",
                expectedPackage, expectedClass));
        this.expectedPackage = expectedPackage;
        this.expectedClass = expectedClass;
    }

    @Override
    protected Judgment evaluate(Path workspace) {
        // Convert package to path: com.example -> src/main/java/com/example
        String packagePath = "src/main/java/" + expectedPackage.replace('.', '/');
        Path packageDir = workspace.resolve(packagePath);
        Path javaFile = packageDir.resolve(expectedClass + ".java");

        boolean dirExists = Files.isDirectory(packageDir);
        boolean fileExists = Files.exists(javaFile);
        boolean hasPackageDecl = false;

        if (fileExists) {
            try {
                String content = Files.readString(javaFile);
                hasPackageDecl = content.contains("package " + expectedPackage);
            } catch (Exception e) {
                return Judgment.error("Failed to read file: " + e.getMessage());
            }
        }

        boolean allPassed = dirExists && fileExists && hasPackageDecl;

        return Judgment.verdict(allPassed)
            .reasoning(allPassed
                ? expectedPackage + "." + expectedClass + " exists with correct package"
                : "Package structure check failed")
            .check(dirExists
                ? Check.pass("package_dir", "Package directory " + packagePath + " exists")
                : Check.fail("package_dir", "Package directory " + packagePath + " missing"))
            .check(fileExists
                ? Check.pass("file_exists", expectedClass + ".java found")
                : Check.fail("file_exists", expectedClass + ".java missing"))
            .check(hasPackageDecl
                ? Check.pass("package_decl", "Correct package declaration")
                : Check.fail("package_decl", "Missing or wrong package declaration"))
            .build();
    }
}
