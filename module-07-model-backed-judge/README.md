# Module 07 - Inside the judgment oracle

```bash
./mvnw exec:java -pl module-07-model-backed-judge
```

[Module 01](../module-01-oracle-boundary) used a model-backed judge and did not open it.
This is the inside, and there is less to it than the name suggests - three independent
parts, composed, no subclassing:

| Part | Job |
|---|---|
| `JudgePromptTemplate` | render the evaluation input from the evidence |
| `EvalModel` | call a backend |
| `JudgmentClassifier` | turn the reply into a `Judgment` |

## The prompt is just text

The demo prints the rendered prompt. `{{goal}}`, `{{output}}` and `{{status}}` are
substituted from the evidence through a `variables` function you write, so nothing is
filled in that you did not name. The builder is a recipe: each `.evidence(run).build()`
makes one ready judge for one agent run.

## The model is the only part that costs anything

The fixture here replays two replies and refuses everything else. In production this one
line becomes `SpringAiEvalModel` (`agent-judge-llm`) or `AgentClientEvalModel`
(`agent-judge-agent-client`). Nothing else in the file changes.

## The classifier is a mapping you write down

```java
.judgmentClassifier(JudgmentClassifiers.passFail("SATISFIED", "NOT_SATISFIED"))
```

Explicit on purpose. A classifier that guessed would be the place where a judge quietly
starts inventing verdicts.

Notice what a Boolean judgment does *not* carry:

```
  status:    PASS
  label:     satisfied
  score:     null
```

No `1.0`. `PASS` already carries that fact, and storing it twice creates two places that
can disagree.

## An unrecognised reply

```
  status:    ABSTAIN
  reasoning: Judge output did not match any label: I am not sure
```

`ABSTAIN`, with the raw text kept in metadata - not a guess, and not a default `PASS`. The
judge reached no finding, which is exactly the case
[module 09](../module-09-error-and-escalation) makes a jury decide about.

## Next

[Module 08](../module-08-jury) puts several of these on a panel - correctly.
