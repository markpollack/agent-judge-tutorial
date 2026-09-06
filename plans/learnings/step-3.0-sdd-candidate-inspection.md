# Step 3.0: Inspect the pinned scheduling candidate

Everything here was measured on the vendored tree at `fc9df4af`, not taken from prior discussion.

## The headline finding: it does not build as delivered

```
[ERROR] Failed to execute goal spring-javaformat-maven-plugin:0.0.47:validate
[ERROR]  * .../security/AccountBootstrapRunner.java
```

The generated system fails the project's own formatting gate. One file, 28 lines. Module 01's
`BuildSuccessJudge` would reject this candidate before a single test ran.

After `spring-javaformat:apply`, the picture changes completely:

```
Tests run: 290, Failures: 0, Errors: 0, Skipped: 4     36.5s
BUILD SUCCESS
```

**290 passing tests and a build that fails on whitespace.** That contrast is the most useful thing
in this fixture, and it lands the least-interpretive-instrument rule harder than any prose: the
cheapest possible check rejects the change, and it says nothing at all about whether the system is
correct. Both facts are true at once and they answer different questions.

## Shape of the implementation

| Package | Java files |
|---|---|
| `scheduling/model` | 31 |
| `scheduling/service` | 25 |
| `security` | 19 |
| `owner` | 16 |
| `scheduling/dto` | 15 |
| `scheduling/repository` | 15 |
| `scheduling/controller` | 12 |
| `scheduling/matching` | 9 |
| `system` | 7 |
| `vet` | 7 |
| `model` | 4 |
| `scheduling/config` | 2 |
| `scheduling/exception` | 1 |

166 main sources, 56 test sources. A `scheduling` subsystem with a controller, service, repository,
dto and model seam, plus a separate `matching` package. That is a hypothesis to check in step 3.2,
not a conclusion: the package names suggest a layering, and whether the code respects it is exactly
what the architecture judge has to establish rather than assume.

## Shape of the specification

7 use cases, **374 acceptance criteria**.

| ID | Use case | Acceptance criteria |
|---|---|---|
| UC1 | secure-scheduling-access | 47 |
| UC2 | configure-clinic-scheduling | 51 |
| UC3 | interpret-appointment-request | 83 |
| UC4 | guide-appointment-selection | 48 |
| UC5 | resolve-staff-fallback | 48 |
| UC6 | manage-appointment-lifecycle | 52 |
| UC7 | notify-audit-and-recover | 45 |

Each use case directory holds `spec.md`, `rules.md` and `criteria.md`. Criteria carry stable
identifiers in the form `UC6-AC5` and a `**Covers:** UC6-B4` back-reference, so requirement identity
survives into a judgment without inventing one.

## Tractable subset for module 06

UC6, manage-appointment-lifecycle. It is self-contained, its vocabulary needs no scheduling theory,
and it contains clauses that are genuinely not a grep. The one to build the module around:

> **UC6-AC5:** When a new appointment starts exactly when another appointment for the same owner or
> pet ends, the system shall not treat the half-open intervals as overlapping.

Answering that requires reading the overlap comparison and deciding whether its boundary is
inclusive. No string search settles it, and a human can check the judge's answer in a minute.

Not cherry-picked to pass: UC6 also contains AC40, "keep completed and cancelled appointments
irreversible", which is a state-machine claim that could easily be unmet.

## Design open question 2, answered

**Can the large candidate run its test suite fast enough for the live path? No.** 36.5s for the
suite, plus the formatter step, plus agent investigation time on 166 files. Modules 06 to 08 are
learning-path material. Their *findings* can be shown live from recordings; their live execution
cannot.

## Provenance re-confirmed

The vendored tree still matches `fc9df4af46171bf7b6146d0477cc68d70e8532ad`, imported 2026-09-06,
Apache 2.0 preserved. Nothing followed the upstream branch. The formatter was applied only to a
throwaway copy under `build/`, never to the vendored fixture, which stays byte-identical to
upstream.

## Consequence for later steps

Module 06 must not use `spec/smart-appointment-scheduling/review.md` as evidence. It is upstream
spec-review material about the specification's own quality, not about whether this implementation
satisfies it.
