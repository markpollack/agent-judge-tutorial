# Step 1.0: Design review and safe branch

## State confirmed before any change

| Fact | Value |
|---|---|
| Prior branch | `code-first-oracle-arc` @ `e26ce6c88c6987c1859c697febeb4c1d6a5b46d4` |
| Merged into `main`? | No. `main` is still `220bc15` |
| New branch | `petclinic-evidence-arc`, cut from `e26ce6c` |
| Working tree at cut | clean |
| Agent Judge target | `0.15.2` |

The 16 prior commits are preserved and were not rewritten, per DD-8.

## Library API inventory

Every production API the design depends on exists and resolves at 0.15.2.

| API | Artifact | Status |
|---|---|---|
| `BuildSuccessJudge.maven(...)` | `agent-judge-exec` | Present. Detects `./mvnw`, falls back to `mvn`. 10 min default timeout |
| `CoveragePreservationJudge(threshold)` | `agent-judge-exec` | Present. Default 5.0 pp |
| `CoverageImprovementJudge(max, min)` | `agent-judge-exec` | Present |
| `JaCoCoReportParser.parse(workspace)` | `agent-judge-exec` | Present. Reads `target/site/jacoco/jacoco.xml` |
| `ModelBackedJudge.builder()` | `agent-judge-ai-core` | Present. template + model + classifier |
| `AgentClientJudgeModel(AgentClient)` | `agent-judge-agent-client` | Present |
| `SimpleJury` / `CascadedJury` / strategies | `agent-judge-core` | Present |

## Incompatibilities and constraints recorded before changing structure

1. **`agent-client-core` is `provided` scope** in `agent-judge-agent-client:0.15.2`, pinned at
   `0.29.3`. The tutorial must declare it explicitly or Stage 2 modules will not resolve at
   runtime. `0.29.3` and `0.30.0` are both in the local repository.

2. **`AgentClientJudgeModel` collapses the request to a single string.** It filters to USER
   messages, joins them, and calls `agentClient.run(goal)`. There is no system-prompt channel and
   no structured output contract, so the whole judging instruction has to travel in one rendered
   prompt and the classifier must parse free text. This shapes the Module 03 design.

3. **The response carries `successful` in metadata.** `AgentClientJudgeModel` records
   `metadata.successful` from `AgentClientResponse.isSuccessful()`. A judge built on it should
   treat `successful == false` as ERROR rather than parsing the text as a verdict, otherwise a
   failed agent run becomes a fabricated verdict.

4. **Running AgentClient from inside a Claude Code session requires `~/scripts/claude-run.sh`**,
   which uses `systemd-run` to escape the process tree. Present and executable. This affects how
   Stage 2 is validated during implementation, not the shipped code.

5. **No `ConjunctiveStrategy` exists in 0.15.2.** The definition-of-done module must compose
   requirements in tutorial code. This is a known product gap, already recorded on the prior
   branch; it is unchanged and is a candidate follow-up.

6. **`CoverageImprovementJudge` floors negative improvement at score 0.0**, so a 1 pp drop and a
   60 pp drop are indistinguishable as scores. Design open question 5 anticipated this. Decision
   recorded in Step 1.3.

## Module inventory: reuse, move, retire

| Existing module | Disposition under the new arc |
|---|---|
| `module-01-oracle-boundary` | Retire from the headline path. Its `ArchitecturalFitJudge` fixture-model approach is superseded by a real AgentClient judge (Step 2.1). Classifier and check-parsing code may be salvaged |
| `module-02-build-and-tests` | Becomes the basis of new Module 01, retargeted at PetClinic |
| `module-03-coverage-evidence` | Becomes the basis of new Module 02, retargeted at PetClinic |
| `module-04-custom-judge` | Demote. Judge authoring is now taught implicitly by the architecture judge |
| `module-05-derived-judge` | Demote or retire. `PackageStructureJudge` is superseded by ArchUnit promotion in Module 08 |
| `module-06-definition-of-done` | Becomes new Module 05, recomposed over PetClinic criteria |
| `module-07-model-backed-judge` | Superseded by the AgentClient judge; keep only if it still earns a slot |
| `module-08-jury` | Becomes new Module 09 |
| `module-09-error-and-escalation` | Becomes new Module 10 |
| `module-10-koog-evaluation` | Becomes new Module 11 |
| `module-11-langchain4j-evaluation` | Becomes new Module 12 |
| `judge-junit` | Keep as optional learning material. Explicitly not a headline concept (vision, out of scope item 3) |
| `test-workspace` | Superseded by `fixtures/petclinic/`. Retire once no module depends on it |

Renumbering happens in Step 5.0, not now, so intermediate steps stay reviewable.

## Local decision

`plans/` was gitignored on the prior branch because the steward owns private planning. The roadmap
requires per-step learning notes inside the tutorial repository, so `.gitignore` now carries an
exception for `plans/learnings/` only. Everything else under `plans/` stays private.

## Open, carried forward

- Whether the large candidate's test suite runs fast enough for the live path (design open
  question 2). Deferred to Step 3.0 when the fixture exists.
- Whether any tutorial-local judge should be promoted into `agent-judge` before Tuesday. Deferred
  to the final report.
