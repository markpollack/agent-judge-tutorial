# agent-judge-tutorial

Two paths, one repository.

| Path | What it is | Where |
|---|---|---|
| **Fundamentals** | The canonical progressive tutorial. Small examples, one concept at a time. Stable numbering. | `module-01-oracle-boundary` … `module-11-langchain4j-evaluation`, plus `judge-junit`, at the root |
| **Case studies** | Applied end-to-end evaluations of real systems. Numbering is local to each. | `case-studies/` |

The root Maven reactor contains **only** the fundamentals path. Case studies build separately:

```bash
./mvnw -o -f case-studies/spec-driven-petclinic/pom.xml test
```

That is deliberate — `spec-driven-petclinic/module-01-build` runs a real 40-second PetClinic build,
and an ordinary fundamentals build should not pay for it.

## Planning lives in the steward repository

**This repository owns code, tests, build inputs and output, releases, public documentation, and
accepted implementation history — and nothing else.**

Vision, design, roadmap state, decisions, reviews, evidence, learnings and work orders live in the
paired steward repository, `markpollack/agent-judge-tutorial-steward`, checked out at
`/home/mark/projects/agent-judge-tutorial-steward`. Its `BINDING.md` is authoritative for the
ownership boundary and the authority order.

**Do not author planning artifacts here.** Between 2026-09-05 and 2026-09-07 a session did exactly
that — a `plans/` trio and seventeen learnings files — and produced two competing sets of
authorities that nobody noticed for two days. They were migrated to the steward on 2026-09-07 and
`plans/` should stay out of this repository.

If you are implementing here, that is correct and expected. Record the *decisions* in the steward,
not beside the code.

## Method

Agento forge: a `VISION` → `DESIGN` → `ROADMAP` trio, stages and steps with explicit entry and exit
criteria, and a learnings file written **after every step**. Templates at
`~/projects/agento-forge/templates/`.

Standing exit criteria for any step, from the roadmap:

- full offline build green
- every claimed number reproducible by a recorded command
- every judge finding hand-verified before it is written down as fact
- a learnings file created under the steward's `plans/learnings/`
- roadmap checkboxes updated
- committed

The first three are honoured routinely here. The last three were skipped on every step of the
PetClinic case study and had to be reconstructed. Do not repeat that.

## Build

```bash
./mvnw -o test                                                   # fundamentals
./mvnw -o -f case-studies/spec-driven-petclinic/pom.xml test      # a case study
```

Always `./mvnw`, never `mvn`. Everything runs offline with no API key: model-backed judges replay
committed recordings. Set `AGENT_JUDGE_TUTORIAL_AGENT=live` for a real agent run.

## Before touching a case study

Read its `README.md` first. `case-studies/spec-driven-petclinic/README.md` opens with a freeze
notice naming what is frozen and which deferrals are deliberate — including duplication between
`EarsJudge` and `Rfc2119Judge` that is **not** to be helpfully refactored.
