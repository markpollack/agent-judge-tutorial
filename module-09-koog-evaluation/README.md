# Module 09: Koog Evaluation

Adapt a Koog `AIAgent` execution into `JudgmentContext`, then evaluate it with an ordinary Agent Judge `Judge`.
The demo uses a deterministic Mockito fake so it runs without a provider or API key; replace the fake with your configured Koog agent in an application.

## Running

```bash
./mvnw exec:java -pl module-09-koog-evaluation
```

The committed Agent Judge target is 0.14.0.
The module uses Koog 1.1.1, the latest non-prerelease GA selected for Agent Judge 0.14.
