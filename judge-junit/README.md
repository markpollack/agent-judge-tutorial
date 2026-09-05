# judge-junit

`Agent Judge outcome → useful JUnit assertion failure.` That is the whole responsibility.

```java
JudgeAssertions.assertPass(judgment);
JudgeAssertions.assertPass(judge, context);
JudgeAssertions.assertPass(verdict);
JudgeAssertions.assertPass(jury, context);

JudgeAssertions.assertFail(judgment);
JudgeAssertions.assertStatus(JudgmentStatus.ABSTAIN, judgment);
```

The `(judge, context)` and `(jury, context)` overloads run the judge and return the
`Judgment` or `Verdict`, so a test can go on to inspect it.

## Why not `assertTrue(judgment.pass())`

Because it throws the evidence away:

```
org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
```

versus:

```
Expected judgment PASS but was FAIL
  reasoning: ReportController does not follow the conventions of this codebase
  2 of 3 checks failed:
    - layering: opens its own java.sql.Connection; HelloController delegates to GreetingService
    - serialization: builds JSON by string concatenation; the codebase returns records
```

## Why not `assertFalse(judgment.pass())`

Because `pass()` is `false` for `FAIL`, `ERROR` **and** `ABSTAIN` alike, so a test written
on it cannot tell a judge that rejected the subject from a judge that never ran:

```
Expected judgment FAIL but was ERROR  (the judge did not complete, so it rejected nothing)
  reasoning: no JaCoCo report found in workspace
```

Every method here compares `JudgmentStatus` exactly.

## For a jury, the members are the diagnosis

```
Expected verdict PASS but was FAIL
  reasoning: Majority vote: 1 passed, 2 failed (majority fail)
  3 judges:
    PASS    build
    FAIL    coverage
        line coverage dropped 64.0 pp (100.0% -> 36.0%), past the 5.0 pp bar
        1 of 1 checks failed:
          - line_coverage_preserved: Drop 64.0% > 5.0% threshold
    FAIL    architectural-fit
        opens its own java.sql.Connection
```

A passing member contributes its name only. A failing one contributes its reasoning and
its failed checks, because that is what you act on.

When a verdict's aggregation evidence shows that fewer judges were counted than were
configured, the message says so. A jury that quietly scored with fewer judges than it
lists is the failure that otherwise reads as a pass.

## Deliberately not here

No thresholds, no retries, no soft assertions, no test-execution telemetry, no
`assertScoreAbove`. The bridge renders an outcome that already exists. Everything else is
policy, and policy belongs in the judge.

## Lambdas

`Judge` and `Jury` are both functional interfaces over `JudgmentContext`, so an implicitly
typed lambda is ambiguous across the two-argument overloads. Name it first - worth doing
anyway, since an unnamed judge has no identity in a verdict:

```java
assertPass(Judges.named(ctx -> ..., "has-tests"), context);
```

## Status

This module is a prototype living inside the tutorial. See the top-level README for
whether it should become a published `agent-judge-junit` artifact.
