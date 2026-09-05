# Module 01 - Where JUnit stops

> The agent says it is done. Should I merge?

```bash
./mvnw exec:java -pl module-01-oracle-boundary   # the demo
./mvnw test -pl module-01-oracle-boundary        # the same boundary, as tests
```

## The subject

The agent was told to "expose a sales report endpoint" and produced
`test-workspace/src/main/java/com/example/ReportController.java`.

## Two kinds of criterion, in one class

`OracleBoundaryTest` is the module. Read it first.

**Known oracle.** An exact answer exists and you can write it down in advance:

```java
@Test
void theControllerExists() {
    assertTrue(Files.exists(WORKSPACE.resolve(REPORT_CONTROLLER)));
}
```

That is not a weakness of Agent Judge, it is the rule: *use the least interpretive
instrument that can reliably answer the question.* Here that is `assertTrue`. Agent Judge
is not a replacement for JUnit, AssertJ, ArchUnit, Checkstyle, JaCoCo, JApiCmp, or the
compiler.

**Judgment oracle.** No expected value exists, so the oracle has to be produced:

```java
Judgment judgment = JudgeAssertions.assertFail(
    ArchitecturalFitJudge.create(), contextFor(REPORT_CONTROLLER));
```

The question is:

> Does this controller fit the architectural conventions and idioms of this codebase,
> without introducing unnecessary complexity?

`ReportController` compiles, sits in the right package, is named correctly, and has the
requested method. Every cheap check says yes. It also opens its own JDBC connection,
concatenates SQL from request parameters, builds JSON by hand, swallows the exception, and
closes nothing - none of which the codebase's other controller does.

## What the judgment keeps

Six criteria, five failing, and none of them averaged away:

```
    FAIL  layering           opens its own JDBC connection, not a service
    FAIL  serialization      builds JSON by hand; the codebase returns records
    FAIL  error-handling     catches Exception and returns an empty result
    FAIL  resource-handling  connection, statement and result set never closed
    FAIL  sql-construction   concatenates request parameters into the SQL
    PASS  naming             class name and package match the codebase
```

Five of six is not `0.17`. A number cannot tell you *which* criterion binds, and the
binding criterion is the only part anybody acts on.

## Credentials

None. `FixtureJudgeModel` replays two hand-written reviews so the module runs offline.
A fixture cannot tell you a judge is *right* - only that the wiring around it is. The
prompt, the classifier, and the `Judgment` are all real; swapping the model for
`SpringAiJudgeModel` is one line.

An unrecorded subject returns `ERROR`, never a default `PASS`. A stand-in that quietly
approves what it has not seen is a judge that cannot fail, which is worse than no judge.

## Honesty about the boundary

One of those six lines is mechanisable: concatenating request parameters into SQL is a
pattern a scanner should own, and [module 05](../module-05-derived-judge) builds exactly
that shape. The *verdict* is the part that is left over - and that is the part worth a
model.

## Next

[Module 02](../module-02-build-and-tests) widens the known oracle: the compiler and JUnit
are already oracles, and Agent Judge can read them.
