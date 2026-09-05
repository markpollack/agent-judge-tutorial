# Module 10 — Evaluating a Koog agent

```bash
./mvnw exec:java -pl module-10-koog-evaluation
```

Everything so far judged a workspace. An agent framework hands you its own result type
instead, and an **evaluator** is the adapter: it runs or wraps that result, puts it in a
`JudgmentContext`, and then applies an ordinary `Judge`.

```java
Judgment judgment = KoogEvaluator.evaluate(agent, "Explain dependency injection", judge);
```

That is the whole bridge. The judge does not know the output came from Koog — which is the
point. The bar you built in [module 06](../module-06-definition-of-done) is the same bar
here.

## Credentials

None. The `AIAgent` is a deterministic Mockito mock, so its output is fixed. The evaluation
path around it is real adapter code.

Mockito's dynamic agent loading prints a JVM warning on Java 21. It is noise, not a
failure; this module is a leave-behind rather than part of the live demo path.

## The committed target

Agent Judge `0.15.2`, Koog `1.1.1`.

## Next

[Module 11](../module-11-langchain4j-evaluation) does the same through a different
framework.
