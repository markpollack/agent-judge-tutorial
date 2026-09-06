# Step 1.2: Module 01, BuildSuccessJudge on a real agent change

## What the module does

Runs the production `BuildSuccessJudge` against the materialized PetClinic city-search candidate
with `./mvnw -o test`, then runs it again with a nonexistent goal so the judge is watched failing.

No tutorial reimplementation. `BuildSuccessJudge.maven("-o", "test")` is the production judge, and
its own wrapper detection resolves `./mvnw` inside the fixture.

## Measured

| | |
|---|---|
| Cold run including materialization | 22.1s |
| Warm run | about 21s |
| `mvnw -o test` alone | 18.1s |
| `mvnw -o test -Dtest=OwnerControllerTests` alone | 11.0s |
| Output | 38 lines, 0 over 80 columns |
| Credentials | none |

## Decisions

**Full test suite over the focused one.** The focused run is 7 seconds cheaper but answers a weaker
question. The full suite answers "did the agent break any of the 71 tests that were already there",
which is the question a reviewer actually has, and 74 versus 71 is a number the presenter can say
out loud. 18 seconds is long for a live opener; the module prints the command and the expected
duration first so the presenter has something to narrate over.

**Offline by default.** Goals are `-o test`, not `test`. `BuildSuccessJudge` joins its goals into
the command line, so passing a Maven flag as a goal works. A build whose answer could depend on the
network is a worse oracle, and this makes the fixture genuinely hermetic.

**The workspace materializes itself.** `PetClinic.candidateWorkspace()` runs the fixture script when
the workspace is missing, so a fresh clone can run module 01 with no setup step. Verified from cold
after deleting the build directory.

**A shared `PetClinic` helper.** The goal string, both workspaces, and the context builder live in
one place in module 01, because every later module evaluates the same subject and should not
re-declare what the agent was asked to do.

## Findings

1. **One build PASS is three facts, and saying so is the whole module.** The change compiles,
   `spring-javaformat` accepted it, and 74 tests passed. The formatter is the interesting one: it is
   a convention that is already machine-checked, which is exactly the case where a judgment oracle
   would be the wrong instrument. That observation sets up modules 03 and 04, where the conventions
   that are not machine-checked are the ones left over.

2. **FAIL and ERROR are already distinguishable here for free.** The nonexistent goal produces FAIL
   because the build ran and said no. Costs 2 seconds and makes the distinction concrete long before
   module 10 has to teach it.

3. **The old module-01 and the new one coexist for now.** `module-01-oracle-boundary` still builds
   and `module-06-definition-of-done` depends on it, so removing it now would break the reactor.
   Both are in the reactor until Step 5.0 does the renumbering the roadmap defers to it. This is
   temporary and slightly confusing in `ls`; it is the least disruptive intermediate state.

## Verification

| Check | Result |
|---|---|
| Uses production judge, not a reimplementation | Yes |
| Passes against the frozen candidate | Yes, PASS in 19s |
| Watched failing | Yes, FAIL on a nonexistent goal |
| No AI or model dependency | Yes |
| Output fits 80 columns | Yes, 0 over |
| Full reactor builds | Yes |
| Integration test | Passes, 7 of 7 required strings |
