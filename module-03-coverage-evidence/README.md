# Module 03 — Coverage as measured evidence

```bash
./mvnw exec:java -pl module-03-coverage-evidence
```

Modules 01 and 02 asked yes/no questions. Coverage is not one. It is a **measurement**,
and a measurement is not an acceptance decision until somebody writes down a bar.

This module keeps three things apart on purpose.

| | |
|---|---|
| **the measurement** | `100.00% → 36.00%`, a `-64.0 pp` delta |
| **the score** | `0.0` — which is a floor, not a distance |
| **the status** | `FAIL`, because 64 pp is past the bar we chose |

## The evidence has to be produced before it can be read

`CoverageImprovementJudge` and `CoveragePreservationJudge` read
`target/site/jacoco/jacoco.xml`. If it is not there, the module runs the test build first.
A measurement with no evidence behind it is not a measurement.

## A score is not a status

`CoverageImprovementJudge` normalizes to `[0.0, 1.0]` and **floors at zero**, so a 1-point
drop and a 64-point drop both score `0.0`. The distance survived only because the judge
also recorded its parts:

```
  the parts it kept:
    FAIL coverage_improved    -64.0 pp (100.0% → 36.0%)
```

Aggregate at read time, never at write time. An aggregate is cheap to recompute from
parts; parts cannot be recovered from an aggregate.

## Where the bar came from

```java
private static final double MAX_ACCEPTABLE_DROP = 5.0;
```

`5.0` is the library's default, and **a default is not a derivation**. A real bar comes
from somewhere: the drop your team has historically waved through, or the point at which
review actually starts. Ours is inherited and provisional, and the comment saying so lives
on the constant — because that is where the next person changing it will be standing.

Setting the bar just under what a known-good run scored is fitting the bar to the data it
has to judge. It will always pass, and you will never know.

## A measurement that could not be taken is not a measurement of zero

Point the judge at a workspace where no build ever ran:

```
  status     ERROR
  reasoning  No JaCoCo report found in workspace — coverage evaluation could not complete
```

`ERROR`, not `FAIL` and not `0.0`. The subject was never judged. Scoring it zero blames
the subject for a missing report, and averaging that zero into anything is worse.
[Module 09](../module-09-error-and-escalation) is where a jury has to decide what to do
about it.

## Reproducing the numbers

Both figures are measured. See [test-workspace/README.md](../test-workspace/README.md).

## Next

[Module 04](../module-04-custom-judge) writes a judge instead of using one.
