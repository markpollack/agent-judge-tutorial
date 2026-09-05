# Module 10: LangChain4j Evaluation

Adapt a LangChain4j `Result<T>` into `JudgmentContext`, then evaluate it with an ordinary Agent Judge `Judge`.
The demo builds a deterministic result locally, so it runs without a model provider or API key.

## Running

```bash
./mvnw exec:java -pl module-10-langchain4j-evaluation
```

The committed Agent Judge target is 0.14.0.
The module uses LangChain4j 1.19.0, the latest non-prerelease GA selected for Agent Judge 0.14.
