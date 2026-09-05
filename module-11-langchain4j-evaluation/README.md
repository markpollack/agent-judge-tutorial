# Module 11 - Evaluating a LangChain4j result

```bash
./mvnw exec:java -pl module-11-langchain4j-evaluation
```

LangChain4j hands back a `Result<T>`. `LangChain4jEvaluator` adapts it into a
`JudgmentContext` and applies an ordinary `Judge` - the same shape as
[module 10](../module-10-koog-evaluation), with a different framework on the far side.

```java
Judgment judgment = LangChain4jEvaluator.evaluate("What is Spring Boot?", supplier, judge);
```

Two frameworks, one evaluation layer, and a judge that does not know or care which one
produced the output it is reading.

## Credentials

None. The `Result<String>` is built directly, so the model call never happens and the
adapter path is still exercised - including finish reason and token usage extraction.

## The committed target

Agent Judge `0.15.2`, LangChain4j `1.19.0`.

## Where this goes

You now have a fixed bar for what *good* means, and a way to apply it to whatever an agent
produces. That is the launch point for the loop this tutorial does not cover:

```
run → record → evaluate → diagnose → change → rerun
```
