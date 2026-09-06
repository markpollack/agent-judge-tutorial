# Step 2.0: Verify the AgentClient judging path

## Result: the live path works

A real agent answered a checkable question through the full production path, with no direct
model-provider client anywhere.

```
  answer     3 findById
  model      claude-sonnet-4-20250514
  metadata   {successful=true, sessionId=7c78d8fa-...}
  elapsed    5s
```

The answer is correct. `OwnerRepository` in the city-search candidate declares three methods and
the last is `findById`. The path exercised is
`AgentClientJudgeModel -> AgentClient -> ClaudeAgentModel -> claude CLI`.

## The bug the smoke check existed to find

The first live run answered:

> There's no `src/` at the workspace root. The path exists only under `fixtures/petclinic/`, in
> four copies with two distinct contents, so a single line would have to pick one arbitrarily.

The agent was running in the tutorial's own checkout, not in the workspace under evaluation, so it
could see all four vendored PetClinic trees and correctly refused to guess which was the subject.

**Cause.** `DefaultAgentClient.determineWorkingDirectory()` resolves in the order explicit request
> goal > **the client's** `defaultOptions` > process working directory. I had set the working
directory on `ClaudeAgentModel.builder()` and on options passed to the *model*. Neither is
consulted. `ClaudeAgentModel` only sees whatever the client already resolved.

**Fix.** Set `defaultOptions` on `AgentClient.builder(model)`, with an absolute normalized path.
Runtime dropped from 17s to 5s as a side effect, because the agent stopped searching four copies.

Two things I got wrong on the way, both worth recording:

1. I assumed the model builder's `workingDirectory` was authoritative. It is a fallback that a
   client-level default overrides, and nothing warns about the shadowing.
2. I hypothesized Claude's project-root detection was walking up to the enclosing git repository
   and tested it by running `git init` in the workspace. It made no difference, which ruled the
   hypothesis out cheaply before I built anything on it.

**This is a judge-correctness issue, not a convenience one.** An architecture judge pointed at the
wrong directory would have reported confidently on the wrong code. A judge whose evidence-gathering
scope is not pinned is not trustworthy, and nothing in the API forces you to pin it.

## The two backends

`JudgeBackends` is the only place the tutorial chooses where an answer comes from.

| Mode | Backend | Used for |
|---|---|---|
| default | `RecordedJudgeModel` | CI and a credential-free clone. Replays a captured answer |
| `AGENT_JUDGE_TUTORIAL_AGENT=live` | `AgentClientJudgeModel` | The live demo and real evaluation |

Both are a `JudgeModel` handed to the same `ModelBackedJudge`, so the prompt, classifier and
`Judgment` are identical. Only the source of the text changes, which is what keeps the recorded
path from bypassing the production architecture.

A missing recording returns `NO_RECORDING` with `successful=false`, which the classifier turns into
ERROR. A stand-in that answers whatever it has not seen is a judge that cannot fail.

## Constraint for every later module

`AgentClientJudgeModel` records `metadata.successful` from `AgentClientResponse.isSuccessful()`. A
judge must check it and return ERROR when it is false, rather than parsing a failed run's text into
a verdict. Recorded in step 1.0 from reading the source; now confirmed present on real responses.

## Running it from inside a Claude Code session

Nested `claude` invocations are blocked by process-tree detection, so every live run in this work
went through `~/scripts/claude-run.sh`, which uses `systemd-run` to escape. This affects how the
tutorial is validated during development, not the shipped code. A presenter running the demo in an
ordinary terminal needs nothing special.

## Verification

| Exit criterion | Result |
|---|---|
| Real AgentClient smoke path succeeds locally | Yes, correct answer in 5s, `successful=true` |
| CI fallback deterministic and labeled test-only | Yes, `RecordedJudgeModel`, javadoc says test-only |
| No direct model-provider client in the tutorial | Confirmed, only `agent-judge-agent-client` plus `agent-client-core` and `agent-claude` |
| Reactor builds | Yes |
