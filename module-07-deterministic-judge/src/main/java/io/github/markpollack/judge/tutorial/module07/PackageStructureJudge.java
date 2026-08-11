package io.github.markpollack.judge.tutorial.module07;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.markpollack.judge.DeterministicJudge;
import io.github.markpollack.judge.context.JudgmentContext;
import io.github.markpollack.judge.result.Check;
import io.github.markpollack.judge.result.Judgment;

/**
 * A reusable deterministic judge that verifies Java package structure.
 * Extends DeterministicJudge for metadata support and granular checks.
 */
public class PackageStructureJudge extends DeterministicJudge {

    private final String expectedPackage;
    private final String expectedClass;

    public PackageStructureJudge(String expectedPackage, String expectedClass) {
        super("package-structure",
            String.format("Verifies %s.%s exists with correct package",
                expectedPackage, expectedClass));
        this.expectedPackage = expectedPackage;
        this.expectedClass = expectedClass;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        // Convert package to path: com.example -> src/main/java/com/example
        String packagePath = "src/main/java/" + expectedPackage.replace('.', '/');
        Path packageDir = context.workspace().resolve(packagePath);
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
