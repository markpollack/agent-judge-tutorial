# test-workspace

The workspace an agent modified. Every judge in this tutorial evaluates this project.

It is a plain Maven project, deliberately: the compiler, Surefire, and JaCoCo are real
oracles, and none of them is part of Agent Judge.

## The convention

`HelloController` is the idiomatic controller. It holds no logic, opens no resource, and
returns a record - it delegates to `GreetingService`. That expectation is the codebase's
convention. Nothing in the build enforces it, and nothing writes it down in a form a
compiler can read.

## The agent's change

`ReportController` is what the agent added for "expose a sales report endpoint". It
compiles, sits in the right package, is named like a controller, and has the method that
was asked for.

It also opens its own JDBC connection, concatenates SQL from request parameters, builds
JSON by hand, swallows the exception, closes nothing, and returns a `String`. Module 01
is the question of whether any of that is visible to the checks you would reach for first.

## Coverage

`GreetingService`, `HelloController`, and `Greeting` are tested. `ReportController` is not.

| State | Line coverage | Lines |
|---|---|---|
| Before `ReportController` | 100.00% | 9 / 9 |
| After `ReportController` | 36.00% | 9 / 25 |

Both numbers are measured, not asserted. Reproduce them with:

```bash
./mvnw clean test                      # writes target/site/jacoco/jacoco.xml
git stash push src/main/java/com/example/ReportController.java && ./mvnw clean test
```

Module 03 reads `target/site/jacoco/jacoco.xml` and uses `100.0` as the baseline.
