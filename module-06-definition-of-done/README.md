# Module 06 — The definition of done

```bash
./mvnw exec:java -pl module-06-definition-of-done
```

Five criteria. Four instruments. Three kinds of oracle. One question.

```
  CRITERION          ORACLE     STATUS
  ────────────────── ────────── ──────
  build              known      PASS
  tests              known      PASS
  coverage           known      FAIL
  package-structure  derived    PASS
  architectural-fit  judgment   FAIL

  done: false
```

**Would you merge this?**

Everyone in the room already knows the answer, and knows it without being told an
aggregation rule. That is the whole point of the module.

## The rule

> **Compose requirements. Aggregate estimates of the same uncertain property.**

These five are not five opinions about one thing. They are five different things, and
every one of them has to hold. A definition of done is **conjunctive** — the aggregate is
a minimum, not a mean.

[Module 08](../module-08-jury) is the other case, where several judges really are
estimating the same property and aggregating them is correct.

## What it must not become

```
  3 of 5 passed = 0.60
```

A mean is a *compensatory* rule: it lets strong criteria offset weak ones. Here that means
"the tests pass" partially making up for "it does not compile", which is not a trade
anybody would accept out loud.

And the number is worth less than the row that failed. `0.60` cannot tell you *which*
criterion binds, and the binding criterion is the only thing you act on.

## `Judges.allOf()` gives you the status and takes the roster

```
  status:    FAIL
  judges invoked: 3 of 5
```

`allOf` short-circuits. Evaluation stopped at `coverage`, so `package-structure` and
`architectural-fit` never ran — and `architectural-fit` would also have failed. You learn
that something failed, not what, and not how much.

Use it for a cheap gate. Do not use it as a definition of done.

## The denominator

```java
private static boolean isDone(List<Judgment> results) {
    return !results.isEmpty()
        && results.stream().allMatch(result -> result.status() == JudgmentStatus.PASS);
}
```

`allMatch` over an empty stream returns `true`. Without the emptiness guard, a definition
of done that lost its criteria would report *done*.

A pass over an empty input set is not a pass. It is an abstention wearing a pass.

## Reuse

The criteria come from the modules that introduced them —
[`ArchitecturalFitJudge`](../module-01-oracle-boundary) and
[`PackageStructureJudge`](../module-05-derived-judge) are imported, not re-written. A judge
you wrote once is a judge you can put in a definition of done.

## Next

[Module 07](../module-07-model-backed-judge) opens up the judgment oracle module 01 used.
