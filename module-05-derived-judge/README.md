# Module 05 — The derived oracle

```bash
./mvnw exec:java -pl module-05-derived-judge
```

A **known** oracle has one exact answer: the build's exit code, the file's presence.

A **derived** oracle has none — there is no single expected value — but several objective
facts together settle the question:

> "Is this class where the codebase's layout says it should be?"

```
  PASS package_dir     Package directory src/main/java/com/example exists
  PASS file_exists     ReportController.java found
  PASS package_decl    Correct package declaration
```

No one of those is the answer. All three are the evidence, and the finding is derived from
them. Nothing here is interpretive, so nothing here needs a model — this is still
deterministic, just not a single assertion.

## Why extend `DeterministicJudge`

Two things a lambda does not give you: metadata (`name`, `description`, `type`) that a
verdict and a log can use, and somewhere natural to put granular `Check`s.

```java
return Judgment.verdict(allPassed)
    .reasoning(...)
    .check(dirExists  ? Check.pass("package_dir", ...) : Check.fail("package_dir", ...))
    .check(fileExists ? Check.pass("file_exists", ...) : Check.fail("file_exists", ...))
    .check(hasDecl    ? Check.pass("package_decl", ...) : Check.fail("package_decl", ...))
    .build();
```

## The rule that makes it useful

> **Every fact computed on the way to the verdict is kept.** A verdict can be recomputed
> from its parts; the parts cannot be recovered from a verdict.

Watch the failing case:

```
Result: FAIL
  PASS package_dir     Package directory src/main/java/com/example exists
  FAIL file_exists     MissingController.java missing
  FAIL package_decl    Missing or wrong package declaration
```

Had this judge stored only `FAIL`, "which part was missing?" would need the agent re-run
rather than a second look at what it already recorded.

That is the same reason [module 01](../module-01-oracle-boundary)'s semantic judge emits
one `Check` per criterion instead of an average.

## Watched red

The second evaluation exists so you see the judge fail. A judge nobody has watched fail is
not yet evidence of anything.

## Next

[Module 06](../module-06-definition-of-done) puts this judge into a definition of done
beside the build, the coverage measurement, and the semantic verdict.
