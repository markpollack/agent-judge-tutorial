# Steps 3.2 and 3.3: The architecture rules, and which of them deserved to become policy

Measured on the pinned candidate at `fc9df4af`, materialized and formatted. The audit ran live
through AgentClient; the recording is committed verbatim.

## The finding

The same code, two documents, two very different answers:

| Document | Requirements | Verdict |
|---|---|---|
| UC6 acceptance criteria | 52 | PASS — 51 pass, 0 fail, 1 undetermined |
| Feature technical design rules | 13 | **FAIL — 5 pass, 8 fail, 0 undetermined** |

**Behaviour conformed. Structure did not.** Spec-driven development produced a system that does
what was asked and is not built the way it was told to be built. No single document could have
asked both questions, and the acceptance criteria — all 52 of them — never came close to the
eight things that are wrong.

I expected the opposite. My prediction going in was that architectural rules would come back
hedged or abstained, because rules like "acquire locks in this global order" are runtime claims
and reading source is a weak way to check them. The judge abstained on nothing and answered all
thirteen with file-and-line citations.

## All eight failures verified by hand

Not sampled. Each one was checked against the code before any of it was used.

| Rule | Claim | Verified |
|---|---|---|
| RULE-1 | `SchedulingRequest.setStatus` unguarded, transitions inline across six services | line 151 is a bare public setter; 13 files call `setStatus` |
| RULE-2 | current time not all Clock-derived, `UserPrincipal:93` cited | 14 `Instant.now()` call sites in main, none in the time service |
| RULE-4 | `StaffFallbackService` locks SchedulingRequest before Owner/Pet/Vet | `lockRequest` at :247 precedes `lockResources` at :258; same at :336/:346 |
| RULE-5 | `LiveFeasibilityService` omits the solver's `exactRejectedPair` constraint | constraint registered in the Timefold provider; zero occurrences of "ejection" in the live service |
| RULE-8 | JPA entities bound straight into views | `model.addAttribute("request", request)` in both controllers, plus `vetRepository.findAll()` |
| RULE-10 | full AI prompt and raw response logged at INFO | `AIInterpretationClient:138` and `:140`, exactly as cited |
| RULE-11 | Maven workflow never runs the database matrix | `maven-build.yml` has `java: ['21']` and nothing else |
| RULE-12 | `requestVersion` threaded and never read | present in the controller, the DTO and two service signatures; absent from both method bodies |

RULE-9 was reported PASS and spot-checks clean: Flyway enabled, automatic baseline off, three
vendor migration directories, no `schema.sql`.

**8 of 8 verdicts and citations correct.**

## The one thing that is not reliable

The free-text counts. "Eight `@PrePersist`/`@PreUpdate` callbacks" for what is actually 13
lifecycle call sites, after module 06's "270 tests" for 290. Twice now, in two runs, on two
documents: the per-requirement verdicts and the file/line citations held up under every check, and
the numbers in the surrounding prose did not.

That is a clean argument for the design the library already has. The `Check` list is the evidence.
The reasoning string beside it is narration, and narration is not verified by anything.

## Step 3.3: 2 of 13

A finding earns promotion when three things are true: it is decidable from bytecode, it caught
something real, and **its mechanised form covers the finding rather than a convenient shadow of
it.** The third test is what disqualifies most candidates.

| Outcome | Rules | Why |
|---|---|---|
| ArchUnit | RULE-1, RULE-2 | structural facts; 5 and 13 real violations; sub-second, offline |
| File assertion | RULE-9, RULE-11 | exact, deterministic, and nothing to do with bytecode |
| Stays with the judge | the other 9 | including RULE-4, RULE-5, RULE-10, RULE-12 |

### Scope is load-bearing, and I got it wrong first

The first draft of the time rule ran unscoped: **16 violations, three of them `LocalDate.now()`
calls in PetClinic's original owner and visit code**, which predates the feature and which RULE-2
says nothing about. Scoped to the feature's two packages it reports 13, which is the number the
judge found.

An architecture test that reports findings nobody intends to fix is one somebody eventually
deletes, and the deletion takes the real findings with it. There is a test pinning the scope.

### The shadows are the interesting half

Two rules that a competent engineer would write, both wrong in instructive ways:

- **RULE-4's shadow is green.** Its checkable half — no solver work inside a transaction — holds.
  The violated half is acquisition *order*, a runtime sequence that bytecode dependency analysis
  has no way to express. Promoting it would have replaced a true finding with a green check.
- **RULE-8's shadow is worse than green.** It reports two private `getAuthenticatedOwner` helpers
  returning an `Owner` internally, which RULE-8 permits, and never sees `Model.addAttribute`,
  whose second parameter erases to `Object`. Two false positives, five real violations missed, in
  the same two files. Acting on it means changing the wrong methods and believing you are done.

A judgment replaced by a check that cannot express it has not been promoted. It has been lost, and
the reassuring result is exactly what makes the loss hard to notice.

## What this says about the maturation loop

The vision's framing — "the first time AI discovers the rule; the next time ArchUnit enforces it" —
is true for 2 of 13 and would be actively harmful for at least 2 more. The judge does not get
retired once the rules exist. It keeps the nine that could not be mechanised, and those include
the two most serious things the audit found: raw owner text in the logs, and an inverted lock
order across two services.
