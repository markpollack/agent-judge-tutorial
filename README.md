# Agent Judge Tutorial

> **Documentation**: https://lab.pollack.ai/docs/agent-judge/tutorial | [API Reference](https://lab.pollack.ai/docs/agent-judge/api-reference)

The agent says it is done. Should you merge?

Part of that question already has an answer you know how to write down: it compiles, the
tests pass, the file is there. Part of it does not.

> **JUnit handles assertions where we know how to write the oracle. Agent Judge extends
> that testing discipline to criteria whose oracle requires richer evidence or judgment.**

Eleven executable Maven modules for **[Agent Judge](https://github.com/markpollack/agent-judge)**,
sequenced by the evaluation problem rather than by the API. No API key, no network, no
model calls.

There are two paths through this repository.

| Path | What it is | Where |
|---|---|---|
| **Learn Agent Judge** | Small progressive examples, one concept at a time | the `module-01`…`module-11` sequence below |
| **Case study** | *Running the Spec You Already Wrote* — a real spec-driven generated system evaluated end to end against requirements written before the implementation | [`case-studies/spec-driven-petclinic/`](case-studies/spec-driven-petclinic/) |

The case study has its own module numbering, local to itself, and its own README with
exact run commands. It complements the progressive tutorial rather than replacing it.

## Prerequisites

- Java 21+
- Maven 3.8+ (or the included `./mvnw`)

## Run it

```bash
git clone https://github.com/markpollack/agent-judge-tutorial.git
cd agent-judge-tutorial
./mvnw install -DskipTests     # once - modules reuse judges from earlier modules
./mvnw exec:java -pl module-01-oracle-boundary
```

Run every module from the **repository root**: the demos and tests resolve `test-workspace`
relative to where Maven was launched.

## The live path

Modules 01 through 06, in directory order. About 18 seconds of runtime, and 10 to 15
minutes of talking.

```bash
./mvnw exec:java -pl module-01-oracle-boundary      # where JUnit stops
./mvnw exec:java -pl module-02-build-and-tests      # the oracles you already trust
./mvnw exec:java -pl module-03-coverage-evidence    # a measurement is not a decision
./mvnw exec:java -pl module-04-custom-judge         # write one: task, criterion, evidence
./mvnw exec:java -pl module-05-derived-judge        # several facts, one finding
./mvnw exec:java -pl module-06-definition-of-done   # would you merge this?
```

01, 02, 03 and 06 carry the narrative. 04 and 05 are the craft beats, showing how a judge
is written before 06 puts five of them in one table. Skip them if the discussion runs
long; the arc still closes without them.

It ends here, which is the whole point of running it:

```
  CRITERION          ORACLE     STATUS
  ------------------ ---------- ------
  build              known      PASS
  tests              known      PASS
  coverage           known      FAIL
  package-structure  derived    PASS
  architectural-fit  judgment   FAIL

  done: false
```

Modules 02, 03 and 06 run real Maven builds. Run each once before presenting so the
dependencies are warm.

No module on this path needs an API key, a network call, or a model endpoint.

## The learning path

| Module | The problem it introduces |
|---|---|
| [01](module-01-oracle-boundary) | A criterion with no expected value. Known oracle vs. judgment oracle. |
| [02](module-02-build-and-tests) | The compiler and JUnit are already oracles; Agent Judge carries their answers. |
| [03](module-03-coverage-evidence) | A measurement is not an acceptance decision. A score is not a status. |
| [04](module-04-custom-judge) | Writing one: task → criterion → evidence → judgment. |
| [05](module-05-derived-judge) | A derived oracle: several objective facts, one finding, all parts kept. |
| [06](module-06-definition-of-done) | Composing requirements without collapsing them into a number. |
| [07](module-07-model-backed-judge) | Inside the judgment oracle: template, model, classifier. |
| [08](module-08-jury) | When aggregation *is* right: several estimates of one property. |
| [09](module-09-error-and-escalation) | A judge that could not run must not improve the result. |
| [10](module-10-koog-evaluation) | The same bar, applied to a Koog agent. |
| [11](module-11-langchain4j-evaluation) | The same bar, applied to a LangChain4j result. |

Plus [`judge-junit`](judge-junit) - a small bridge turning an Agent Judge outcome into a
useful JUnit assertion failure.

## Three kinds of oracle

> **The mistake is treating every eval as an LLM-as-a-judge problem. Start by asking what
> kind of oracle you actually have.**

| Kind | There is | Instrument | Modules |
|---|---|---|---|
| **Known** | an exact answer | the compiler, JUnit, JaCoCo, a deterministic judge | 01, 02, 03 |
| **Derived** | no single expected value, but objective evidence | several checks, one finding | 05 |
| **Judgment** | a property that requires interpretation | a model-backed judge | 01, 07 |

Most criteria that feel like they need a model do not. The sequence reveals this through
code rather than teaching it up front.

## The two rules the sequence is built on

**Use the least interpretive instrument that can reliably answer the question.** Agent
Judge does not replace JUnit, AssertJ, ArchUnit, Checkstyle, JaCoCo, JApiCmp, or the
compiler. Module 01 keeps four criteria in plain `assertTrue` on purpose.

**Compose requirements. Aggregate estimates of the same uncertain property.** A definition
of done is a conjunction, not a vote - `3 of 5 = 0.60` lets "the tests pass" offset "it
does not compile". Module 06 is the first rule; module 08 is the second.

## Subject under evaluation

[`test-workspace/`](test-workspace) is a small Maven project an agent modified. Its
`HelloController` is the codebase's idiom; its `ReportController` is what the agent
produced. Everything in the tutorial judges that change.

## Versions

| | |
|---|---|
| Agent Judge | `0.15.2` |
| Koog | `1.1.1` |
| LangChain4j | `1.19.0` |

## Tests

Modules 01 and `judge-junit` carry JUnit tests. Everything else is a `main`.

```bash
./mvnw test
```

## Integration testing

A credential-free suite that runs each module and asserts its required output.

```bash
cd integration-testing
./scripts/run-integration-tests.sh            # everything
./scripts/run-integration-tests.sh --demo     # the live path only
jbang RunIntegrationTest.java module-01-oracle-boundary
```

Optional Claude validation runs only when `AGENT_JUDGE_TUTORIAL_AI_VALIDATE=true`.
Candidate verification can select an isolated repository and pre-release artifact with
`AGENT_JUDGE_TUTORIAL_MAVEN_REPO` and `AGENT_JUDGE_TUTORIAL_AGENT_JUDGE_VERSION`; the
committed examples target 0.15.2.

## Related

- [Agent Judge](https://github.com/markpollack/agent-judge) - the library
- [Documentation](https://lab.pollack.ai/docs/agent-judge/getting-started)
- [Agent Experiment](https://github.com/markpollack/agent-experiment) - the runner that uses these juries
