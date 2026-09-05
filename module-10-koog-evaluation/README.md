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

Mockito's inline mock maker loads a Java agent dynamically, which makes Java 21 print four
warnings and a CDS notice. `.mvn/jvm.config` at the repository root turns them off
(`-XX:+EnableDynamicAgentLoading -Xshare:off`) so the output stays readable.

## The committed target

Agent Judge `0.15.2`, Koog `1.1.1`.

## Next

[Module 11](../module-11-langchain4j-evaluation) does the same through a different
framework.
