# Step 1.3: Module 02, coverage as gathered measured evidence

## Vision assumption 4 is wrong, and the truth is a better lesson

The vision assumed PetClinic carries no JaCoCo instrumentation, so adding it would demonstrate
engineering the oracle. It does carry JaCoCo, version 0.8.15, in both the baseline and the large
candidate.

What is true instead is sharper. The `report` goal is bound to `prepare-package`, which runs after
`test`. So `./mvnw test` leaves `target/jacoco.exec` and no XML at all, and
`JaCoCoReportParser` reads `target/site/jacoco/jacoco.xml`. **The instrumentation is present and
the evidence is still not produced by the phase you ran.**

That is a more realistic version of the same lesson, and it costs nothing to fix:
`./mvnw -o test jacoco:report` is 18.2s against 18.1s for `test` alone. Module 01 now uses that
goal, so it leaves behind the evidence module 02 measures, and module 02 runs in 1.1s.

No instrumentation was added to the fixture. The vendored trees stay byte-identical to upstream.

## Measured numbers, all reproduced not assumed

| State | Line coverage | Lines |
|---|---|---|
| Baseline `88e37c15` | 94.27% | 296 / 314 |
| City-search candidate | 94.53% | 311 / 329 |
| Seeded, `OwnerControllerTests` deleted | 82.37% | 271 / 329 |

The candidate improves coverage by 0.26 pp, because the agent added 15 production lines and tests
that reach them. The seeded regression drops 11.90 pp against baseline.

## Decisions on the two design open questions

**Open question 4, the threshold.** Chosen: 0.0 pp, not the library default of 5.0. Derivation
recorded on the constant: the agent was asked to add a feature, and new production code its author
did not test is exactly what a coverage drop detects here, so any drop is the signal. 5.0 pp is a
reasonable tolerance for a refactor across a large codebase and the wrong bar for one small
feature. A default is not a derivation.

The bar still passes on the real candidate at +0.26, so it is not fitted to make the demo green.
It fails the seeded case by a wide margin.

**Open question 5, `CoverageImprovementJudge`.** Not used. Its normalized score floors negative
improvement at 0.0, so a 1 pp drop and a 60 pp drop are indistinguishable as scores, which would
actively confuse a module about measurement fidelity. `CoveragePreservationJudge` carries the real
delta in metadata and is the whole lesson. Recorded as a product follow-up.

## Recorded evidence rather than a second build

The failing case uses `fixtures/petclinic/evidence/seeded-no-owner-tests.jacoco.xml`, a genuine
JaCoCo report from a real build of the candidate with its test class removed. It is committed with
the exact command that reproduces it, and the module copies it into a temporary workspace at the
path the parser reads.

This keeps the module at 1.1s while still watching the judge go red on a real 11.9 pp regression.
It is recorded evidence, not a fixture invented to fail.

## Findings

1. **All four outcome states now appear on real inputs across modules 01 and 02**: PASS, FAIL from
   a real regression, FAIL from a real non-zero exit code, and ERROR from genuinely absent evidence.
   None of them is simulated.

2. **The judgment metadata is the module.** Baseline, current, drop and threshold all survive in
   the judgment. Raw doubles print as `94.52887537993921`, so the demo formats to two decimals;
   the stored value keeps full precision.

3. **A library reasoning string contains an em dash.** `CoveragePreservationJudge` emits
   "No JaCoCo report found in workspace — coverage evaluation could not complete". That is
   `agent-judge` text, not authored tutorial text, so it is out of scope for the no-em-dash rule
   but it does reach the terminal. Noted as a cosmetic product follow-up.

## Verification

| Check | Result |
|---|---|
| Real JaCoCo report parsed | Yes |
| Baseline, current, delta, threshold visible | Yes |
| Judge watched failing | Yes, FAIL at 11.9 pp on a real recorded report |
| Judge watched erroring | Yes, ERROR on a workspace with no report |
| No averaging taught | Correct, no numeric aggregation appears |
| No AI or model dependency | Yes |
| Runtime warm | 1.1s |
| Output | 54 lines, 0 over 80 columns |
| Integration test | Passes, 11 of 11 required strings |
