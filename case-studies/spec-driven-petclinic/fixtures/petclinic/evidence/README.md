# Recorded evaluation evidence

Real output from real builds, committed so a module can show a failing case without paying
for another 18 second build. Every file here is reproducible by the command that made it.

## `seeded-no-owner-tests.jacoco.xml`

A genuine JaCoCo report from the city-search candidate with `OwnerControllerTests.java` deleted:
the agent adds the feature and ships no tests for it.

Reproduce:

```bash
./materialize-city-search.sh build/seeded
rm build/seeded/src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java
cd build/seeded && ./mvnw -o test jacoco:report
# target/site/jacoco/jacoco.xml is this file
```

| | Line coverage | Lines |
|---|---|---|
| Baseline (`88e37c15`) | 94.27% | 296 / 314 |
| City-search candidate | 94.53% | 311 / 329 |
| Seeded, no owner tests | 82.37% | 271 / 329 |

The seeded drop against baseline is 11.90 percentage points.

This is recorded evidence, not a fixture invented to fail. Module 02 uses it so the coverage
judge can be watched going red in front of an audience.
