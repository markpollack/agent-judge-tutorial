# DEMO-SETUP — superseded

> # ⚠️ You want [`DRY-RUN.md`](DRY-RUN.md).
>
> **That is the one document for the conference demo.** It covers preflight, the IntelliJ walkthrough,
> and the terminal fallback.

This file used to be a second, terminal-era checklist. Having two documents for one demo caused
exactly the failure you would expect — the wrong one gets opened, and it carries stale commit SHAs
nobody remembered to update. It is a stub now so that opening it costs two seconds instead of
sending you down a superseded path.

Everything it contained lives in `DRY-RUN.md`:

| What you were looking for | Where it is now |
|---|---|
| Preconditions, SHA and clean-tree check | `DRY-RUN.md` **Part A1** — one script, `./dry-run-check.sh` |
| Pre-materialize and warm the candidate | `DRY-RUN.md` **Part A1** — `./dry-run-check.sh --warm` |
| The demo itself | `DRY-RUN.md` **Part B** — IntelliJ + JUnit |
| Terminal command sequence | `DRY-RUN.md` **Part C** — emergency fallback |
| Expected results | `DRY-RUN.md` — *If anything looks wrong* |
| Timings | `DRY-RUN.md` — *Timing* |
| Spoken transitions | `DRY-RUN.md` **Part B**, inline at the step each belongs to |

The full previous version is in git history if it is ever wanted:

```bash
git show ef31378:case-studies/spec-driven-petclinic/DEMO-SETUP.md
```
