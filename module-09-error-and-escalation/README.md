# Module 09 — Error, abstention, and escalation

```bash
./mvnw exec:java -pl module-09-error-and-escalation
```

One rule, in the shape you already trust from testing:

> A judge that failed, timed out, was never configured, or declined to answer must not make
> the result look better.

A broken **test** fails closed — it goes red and you fix it. A broken **judge** fails
open: it returns a plausible number and you build on it. Nothing checks the checker, so
fail-closed has to be chosen rather than assumed.

## Three judges, one of which cannot run

`CoveragePreservationJudge` is pointed at a workspace where no build ever ran. This is the
same judge module 03 used, and the `ERROR` is real:

```
  build      PASS     exit code 0
  tests      PASS     4 tests, 0 failures
  coverage   ERROR    No JaCoCo report found in workspace
```

## The same panel, under each `ErrorPolicy`

```
  POLICY             STATUS   ROSTER
  ------------------ ------   ----------
  PROPAGATE          ERROR    0 of 3
  TREAT_AS_FAIL      PASS     3 of 3
  TREAT_AS_ABSTAIN   PASS     2 of 3
  IGNORE             PASS     2 of 3
```

**Three of four report `PASS`** for a run in which a required criterion was never
evaluated.

`TREAT_AS_FAIL` is the surprise. It *did* convert the error into a failure, the roster is
intact at 3 of 3 — and the verdict is still `PASS`, because majority voting is
compensatory and two passes outvote one failure.

**The error policy and the aggregation rule are two separate decisions**, and getting one
right does not save you from the other. That is
[module 06](../module-06-definition-of-done)'s point arriving from the other direction.

Only `PROPAGATE` fails closed, and it is the default. That is the one piece of luck in the
table. Choose on purpose.

## The roster is the tell

Under `IGNORE`, the verdict still names all three judges and the aggregate was computed
from two:

```
  aggregate status: PASS
  named members:    [build, tests, coverage]
  inputCount:       3
  eligibleCount:    2
```

Nothing is hidden — both counts are recorded — but nothing raises its voice either.

> **Assert `eligibleCount == inputCount` wherever you expect a full panel.** That single
> equality is the difference between a jury that scored what you asked for and one that
> scored what survived.

## Abstention is not agreement

```
  status:    ABSTAIN
  reasoning: No baselineCoverage in metadata
  pass():    false
```

`pass()` is `false` for `FAIL`, `ERROR` **and** `ABSTAIN` alike. A consumer that branches
on `!pass()` records this as a rejected subject when nothing was ever asked — the judge
stops lying and the system starts blaming the subject.

Assert `status()`, not `pass()`. That is why
[`JudgeAssertions`](../judge-junit) compares statuses exactly.

## Escalation

`CascadedJury` orders juries into tiers so a subject already rejected costs nothing
further:

```
  a subject that survives the cheap tier:
    /structural   REJECT_ON_ANY_FAIL  PASS
    /content      FINAL_TIER          FAIL

  a subject that does not:
    /structural   REJECT_ON_ANY_FAIL  FAIL
```

Only `/structural` appears for the second subject. Which tiers are present is itself the
record, so a tier that never ran is never mistaken for a tier that passed.

A cascade is **cost control**. It is not an evaluation policy and it changes no judge's
decision.

## Next

[Module 10](../module-10-koog-evaluation) applies the same bar to a real agent framework.
