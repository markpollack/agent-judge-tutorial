# Module 02 — The build and the tests are already oracles

```bash
./mvnw exec:java -pl module-02-build-and-tests
```

Module 01 needed a model because the criterion had no expected value. Most criteria are
not like that.

The compiler decides whether the code compiles. JUnit decides whether the tests pass. Both
are exact, both are already trusted, and neither is part of Agent Judge.

```java
Judge compiles = BuildSuccessJudge.maven("compile");
Judge tested   = BuildSuccessJudge.maven("test");
```

`BuildSuccessJudge.maven()` prefers `./mvnw` in the workspace and falls back to `mvn` on
`PATH`. It runs the real command in the real workspace and reads the exit code. **The exit
code is the oracle; the judge is the wrapper around it.**

## Watched red

The third case runs a goal that does not exist, so Maven exits non-zero:

```
--- And when the oracle says no ---
  status     FAIL
  reasoning  Command failed. Expected exit code 0 but got 1
```

A judge nobody has watched fail is not yet evidence of anything.

## Why wrap an oracle you already trust

Not to improve it. To make its answer travel.

[Module 06](../module-06-definition-of-done) needs a build outcome, a coverage
measurement, and a semantic verdict to sit in one definition of done. They can only do
that if they arrive in the same shape.

## Next

[Module 03](../module-03-coverage-evidence) reads an oracle that returns a number rather
than a yes.
