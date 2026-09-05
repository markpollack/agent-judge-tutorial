# Module 08 — When aggregation is the right answer

```bash
./mvnw exec:java -pl module-08-jury
```

[Module 06](../module-06-definition-of-done) refused to aggregate: five criteria, five
different questions, every one required. Voting on those would let "the tests pass"
outvote "it does not compile".

This is the other case, and it is the one a jury is actually for.

> **Compose requirements. Aggregate estimates of the same uncertain property.**

One question, asked three times:

```
  "Does ReportController fit this codebase's conventions and idioms?"

  reviewer-a  (weighs layering)     FAIL
  reviewer-b  (weighs simplicity)   FAIL
  reviewer-c  (weighs consistency)  PASS
```

Three independent estimates of **one** property. Combining them says something the
individual answers did not.

## Majority

```
  FAIL — Majority vote: 1 passed, 2 failed (majority fail)
```

A legitimate aggregate: two of three independent reviewers found the same problem.

## Consensus

```
  ABSTAIN — No consensus: 1 passed, 2 failed among 3 applicable judge(s)
```

`ABSTAIN`, not `FAIL`. **A split vote is indeterminate, not negative.** The panel
disagreed, which is a different fact from the panel rejecting the subject, and it usually
calls for a human rather than a merge button.

Rejection belongs to the gate, not to the aggregation step.

## The estimates survive the aggregate

```
  reviewer-a   FAIL   reaches the database directly, unlike its neighbours
  reviewer-b   FAIL   hand-rolled SQL, JSON and error handling
  reviewer-c   PASS   package, class name and method shape all match
```

A verdict recording only `FAIL, 2 of 3` could not tell you who disagreed or why — the
first thing worth reading when a panel splits.

⚠️ Note the key space: `weights` is keyed by the judge's **position** in the builder, while
`individualByName()` is keyed by its **name**. Join them by order, not by name.

## What this module deliberately does not do

There is no weighted average, and its absence is the point.

These three judges made no measurement. Averaging them means reading `PASS` as `1.0` and
`FAIL` as `0.0` and calling the result `0.33` — a number invented by the aggregation step,
carrying a precision nobody measured, comparable against thresholds nobody derived.

Use `AverageVotingStrategy`, `WeightedAverageStrategy` or `MedianVotingStrategy` when the
judges genuinely produce scores, as [module 03](../module-03-coverage-evidence)'s coverage
judge does. Weight one reviewer above another only when you can say what the weight is for.

## Next

[Module 09](../module-09-error-and-escalation) asks what happens when one of these judges
cannot run at all.
