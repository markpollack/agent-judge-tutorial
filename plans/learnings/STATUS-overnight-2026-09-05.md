# Status: PetClinic evidence arc, overnight run

> Branch `petclinic-evidence-arc` @ `c7daf5c`, pushed. Cut from `code-first-oracle-arc@e26ce6c`,
> which is untouched. `main` untouched at `220bc15`. Nothing merged.

## Roadmap progress

| Stage | Step | State |
|---|---|---|
| 1 | 1.0 Design review and safe branch | done |
| 1 | 1.1 Vendor pinned PetClinic fixtures | done |
| 1 | 1.2 Module 01, BuildSuccessJudge | done |
| 1 | 1.3 Module 02, coverage evidence | done |
| 2 | 2.0 Verify AgentClient path | done |
| 2 | 2.1 Module 03, first AI architecture judge | done |
| 2 | 2.2 Module 04, agentic investigation | done |
| 2 | 2.3 Module 05, definition of done | done |
| 3 | 3.0 Inspect scheduling candidate | done |
| 3 | 3.1 Module 06, SpecConformanceJudge | **not started** |
| 3 | 3.2 Module 07, architecture at scale | not started |
| 3 | 3.3 Module 08, ArchUnit promotion | not started |
| 4 | 4.0 to 4.4 jury, errors, Koog, LangChain4j | not started |
| 5 | 5.0 to 5.2 consolidation, conference path, handoff | not started |

**9 of 20 steps. Stages 1 and 2 complete, Stage 3 begun.**

## Verified state

| Check | Result |
|---|---|
| Full Maven reactor | BUILD SUCCESS |
| Unit tests | 15 passing |
| Integration suite | **16 of 16 passing**, 0 failed, 0 skipped |
| New modules 01 to 05 | all run, all inside 80 columns, no logging noise |
| Vendored fixtures | byte-identical to upstream, Apache 2.0 preserved |
| Credentials required | none; live agent mode is opt-in |

## What the new modules are

| Module | Question | Oracle | Runtime |
|---|---|---|---|
| 01 build-judge | Does it build and pass tests? | known | 22s |
| 02 coverage-evidence | What does measured coverage say? | measured | 1.1s |
| 03 ai-architecture-judge | Does it fit, given curated evidence? | judgment | 1.2s recorded |
| 04 agentic-architecture-judge | Does it fit, if the judge investigates? | judgment | 2.2s recorded |
| 05 definition-of-done | All three at once. Merge? | composed | 21s |

The old `code-first-oracle-arc` modules still build and still pass their integration tests. They are
removed or renumbered at Step 5.0, not before, because module 06 of the old arc depends on module 01
of the old arc and removing it early would break the reactor.

## The five findings that matter most

**1. The AI judge found three real defects in a fixture I wrote.** The repository Javadoc was
inserted between `findById`'s Javadoc and `findById` itself. The change added an endpoint with no
view, so it was unreachable through the form the convention uses. And it added no
`ClinicServiceTests` case for its new finder while every pre-existing finder has one. All three were
cited with file and line, all three were verified by hand, all three were real. An AI judge improved
a human-written change three times before it was used to teach anything.

**2. Module 04 justified its own existence empirically.** Given curated evidence, module 03 said
PASS. Allowed to read the codebase, module 04 said FAIL, for a reason the curated set could not have
contained, because the human who chose that evidence had already decided the answer. That was not
designed; my evidence set was incomplete in exactly the way the module warns about.

**3. The judgment oracle is self-inconsistent, measured.** Eight live runs on identical input. On
the final conventional change it disagrees with itself roughly one run in three, and every verdict
was defensible with checkable citations. Recorded replay is therefore the demo default. This is the
strongest possible motivation for the jury module in Stage 4, which now has real measured
disagreement behind it.

**4. The large generated candidate does not build.** It fails `spring-javaformat` on one file and 28
lines. After the formatter runs, 290 tests pass. The cheapest possible check rejects it and says
nothing about correctness.

**5. Agent working directory is a judge-correctness issue.** `DefaultAgentClient` resolves the
working directory from the **client's** default options; setting it on the model is silently
ignored. Before the fix the judge was reading the tutorial's own checkout and could see four copies
of PetClinic. A judge whose evidence scope is not pinned reports confidently on the wrong code, and
nothing in the API forces you to pin it.

## Decisions taken without asking, all reversible

| Decision | Reason |
|---|---|
| Coverage threshold 0.0 pp, not the library default 5.0 | The task was to add a feature; untested new code is exactly what a drop detects. 5.0 is a refactor tolerance |
| `CoverageImprovementJudge` not used | Its score floors negative improvement at 0, so a 1 point and a 60 point drop are identical. Would undercut a module about measurement fidelity |
| Small change stored as a patch, not a third tree | One copy of PetClinic per genuinely different codebase |
| Recorded agent replay is the default | A verdict that flips 1 run in 3 cannot be a demo punchline. Live is one environment variable away |
| Module 05 ships with a genuinely failing criterion | The gap is real. A manufactured green table would be weaker and dishonest |
| New modules added alongside old ones | Old module 06 depends on old module 01; renumbering is Step 5.0's job |
| `plans/learnings/` un-ignored in the project repo | The roadmap requires per-step learning notes there. The rest of `plans/` stays private |

## Product follow-ups for `agent-judge`

1. **No conjunctive strategy.** A definition of done is hand-written in every consumer. A criteria
   set that keeps requirements visible and refuses to vote is the clearest gap.
2. **`CoverageImprovementJudge` floors negative deltas**, losing the distance that matters.
3. **`AgentClientJudgeModel` has no system-prompt channel and no structured-output contract.** The
   whole judging instruction travels in one rendered user message and the classifier parses free
   text. `ClaudeAgentOptions` supports `jsonSchema`, which the adapter does not expose.
4. **Working-directory shadowing** between `AgentClient` and `AgentModel` defaults, described above.
5. **A library reasoning string contains an em dash**, which reaches the terminal.

## To resume

Start at Step 3.1. Read this file and `plans/learnings/step-3.0-sdd-candidate-inspection.md`, which
names the tractable spec subset (UC6, and `UC6-AC5` as the clause that is not a grep).

Live agent runs need `~/scripts/claude-run.sh` from inside a Claude Code session, and
`AGENT_JUDGE_TUTORIAL_AGENT=live`. Add `AGENT_JUDGE_TUTORIAL_CAPTURE=<recording-name>` to refresh a
recording from a real run.
