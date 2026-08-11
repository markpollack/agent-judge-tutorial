# Agent Judge Tutorial

> **Documentation**: https://lab.pollack.ai/docs/agent-judge/tutorial | [API Reference](https://lab.pollack.ai/docs/agent-judge/api-reference)

A progressive, hands-on tutorial for learning **[Agent Judge](https://github.com/markpollack/agent-judge)** — the framework-neutral evaluation layer for AI agent output.

## Prerequisites

- Java 21+
- Maven 3.8+ (or use the included `./mvnw` wrapper)
- No API key required — all modules run locally

## Getting Started

### Step 1: Build All Modules

```bash
git clone https://github.com/markpollack/agent-judge-tutorial.git
cd agent-judge-tutorial
./mvnw compile
```

### Step 2: Run Any Module

```bash
# Module 01: Your first judge
./mvnw exec:java -pl module-01-single-judge

# Module 04: Multi-judge jury with voting
./mvnw exec:java -pl module-04-simple-jury

# Module 08: Composed AI judge (stub model, no API key)
./mvnw exec:java -pl module-08-model-backed-judge

# Module 09: Evaluate a Koog agent result (deterministic fake, no API key)
./mvnw exec:java -pl module-09-koog-evaluation

# Module 10: Evaluate a LangChain4j result (deterministic fake, no API key)
./mvnw exec:java -pl module-10-langchain4j-evaluation
```

## Tutorial Structure

### Part 1: Core Evaluation

| Module | Title | What You'll Learn |
|--------|-------|-------------------|
| 01 | Single Judge | JudgmentContext, FileExistsJudge, pass/fail |
| 02 | Build Judge | BuildSuccessJudge runs real Maven builds |
| 03 | Composition | Judges.and(), or(), allOf(), anyOf() |
| 04 | Simple Jury | Weighted judges, majority and average voting |
| 05 | Cascaded Jury | Tiered evaluation, fail-fast cost optimization |

### Part 2: Custom Judges

| Module | Title | What You'll Learn |
|--------|-------|-------------------|
| 06 | Lambda Judge | Inline lambdas, named lambdas with metadata |
| 07 | Deterministic Judge | DeterministicJudge subclass with granular Checks |

### Part 3: AI-Backed Judges

| Module | Title | What You'll Learn |
|--------|-------|-------------------|
| 08 | ModelBackedJudge | Composed AI judge: template + model + classifier |
| 09 | Koog Evaluation | Adapt a Koog `AIAgent` execution into the shared evaluation layer |
| 10 | LangChain4j Evaluation | Adapt a LangChain4j `Result<T>` into the shared evaluation layer |

## Build Commands

```bash
# Build everything
./mvnw compile

# Build a specific module
./mvnw compile -pl module-04-simple-jury

# Run a specific module
./mvnw exec:java -pl module-04-simple-jury
```

## Integration Testing

The tutorial includes a credential-free automated test suite.
Its required-output assertions are the default gate.
Optional Claude validation runs only when `AGENT_JUDGE_TUTORIAL_AI_VALIDATE=true` is explicitly set.
Candidate verification can select an isolated Maven repository and exact pre-release artifact with
`AGENT_JUDGE_TUTORIAL_MAVEN_REPO` and `AGENT_JUDGE_TUTORIAL_AGENT_JUDGE_VERSION`; committed examples
continue to target 0.14.0.

```bash
cd integration-testing

# Run all tests
./scripts/run-integration-tests.sh

# Run core evaluation tests only
./scripts/run-integration-tests.sh --core

# Run a single module test
jbang RunIntegrationTest.java module-01-single-judge
```

## Project Structure

```
agent-judge-tutorial/
├── test-workspace/                  # Shared Maven project for judges to evaluate
├── module-01-single-judge/          # FileExistsJudge, JudgmentContext
├── module-02-build-judge/           # BuildSuccessJudge with real builds
├── module-03-composition/           # Boolean judge composition
├── module-04-simple-jury/           # SimpleJury with voting strategies
├── module-05-cascaded-jury/         # CascadedJury with tiered evaluation
├── module-06-lambda-judge/          # Lambda and named lambda judges
├── module-07-deterministic-judge/   # DeterministicJudge subclass with Checks
├── module-08-model-backed-judge/    # ModelBackedJudge composed pipeline
├── module-09-koog-evaluation/       # Credential-free Koog bridge example
├── module-10-langchain4j-evaluation/# Credential-free LangChain4j bridge example
└── integration-testing/             # Credential-free deterministic test suite
```

## Related Projects

- [Agent Judge](https://github.com/markpollack/agent-judge) — The evaluation library this tutorial teaches
- [Agent Judge Documentation](https://lab.pollack.ai/docs/agent-judge/getting-started) — Full docs with getting started guide
- [Agent Experiment](https://github.com/markpollack/agent-experiment) — Experiment runner that uses Agent Judge juries
