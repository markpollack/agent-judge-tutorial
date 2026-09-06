# The tier-two fan-out: does investigating a judge's leads actually work?

A measurement, not an assertion. The eight architectural findings from module 07 had already been
verified by hand, so ground truth existed before the experiment ran.

## Design

Eight fresh `general-purpose` agents, one per violated rule, no shared context, no knowledge of the
hand verification. Each received the rule verbatim, the judge's one-line lead verbatim, and the
workspace path.

Two prompt decisions did the work:

1. **Framed on consequence, not verification.** "YOUR JOB IS NOT TO VERIFY THAT LEAD. Establish its
   CONSEQUENCE and whether that consequence is REACHABLE." Downgrading was explicitly invited and
   named as a valuable result.
2. **Citation and command discipline**, the rule tier one lacked: every claim carries a `file:line`;
   every number comes from a command, and the command is shown.

## Result

| Rule | Verdict | Severity | What changed |
|---|---|---|---|
| RULE-1 | ESCALATE | high | 72 sites / 8 classes / 5 enums, plus two live defects |
| RULE-4 | ESCALATE | high | ABBA confirmed, counterparty named, reachability **narrowed** |
| RULE-10 | ESCALATE | high | purge attestation false; a test pins the leak in |
| RULE-11 | ESCALATE | high | both clauses violated; lead **corrected** in one direction |
| RULE-12 | ESCALATE | high | reachable corruption chain in a method nobody examined |
| RULE-2 | ESCALATE | medium | 12 audit stamps, plus one fail-open untested auth control |
| RULE-5 | ESCALATE | medium | two divergences not one; two others **cleared** |
| RULE-8 | DE-ESCALATE | low | latent, not live — but a different live leak found |

**7 escalate, 1 de-escalate, 0 confirmed-as-stated, 0 fabrications detected** on spot-check.

Every claim I re-checked by hand held: the write/read clock split, the missing UNIQUE constraint in
all three vendors, `fullAiResponse` never populated, `STAFF_OFFERED` written only by a test, and the
inverted log assertion.

## The de-escalations matter more than the escalations

A fan-out that only confirms is theatre. Four separate downgrades happened *inside* escalations:

- **RULE-11** stated plainly that the lead was wrong — CI *does* run the vendor tests, because
  `ubuntu-latest` ships Docker. It then found two things the lead missed.
- **RULE-4** narrowed reachability: two different requests belonging to the same owner do **not**
  deadlock, because the counterparty never wants the first request's row. The cycle requires the
  same `SchedulingRequest`.
- **RULE-5** cleared two apparent divergences as unreachable with evidence, and noted two solver
  constraints are structurally dead because the problem factory hardcodes their flags.
- **RULE-1** ruled out an exploit path it had constructed, on finding that every confirm path
  deletes the staff claim.

RULE-8's full downgrade turned on one command: `fullAiResponse`, the most alarming field on the
bound entity, is **never populated** — its only two writes in `src/main` are
`setFullAiResponse(null)`. Unlearnable by reading the entity, the controller, or the template.

## What the second tier produced that the first could not

The judge emitted **addresses**. The fan-out emitted **consequences**, and consequences are a
different kind of object:

- **RULE-12** is the clearest case. The lead pointed at `acceptGuidedHold`. The consequence is in
  `rejectGuidedHold`, which checks only `REJECTED` — so accept, browser Back, resubmit Reject
  orphans a `BOOKED` appointment, and re-matching then inserts a second one on a `request_id` that
  carries no UNIQUE constraint in h2, postgres or mysql, permanently breaking
  `Optional<Appointment> findByRequestId`. One owner, no race.
  **Fixing the address would have fixed the wrong method.**
- **RULE-1** produced two shipped defects from a structural complaint: `RequestStatus.STAFF_OFFERED`
  is read by six production guards and written by zero production code (only
  `LifecycleProcessorTests.java:163` writes it), so the offer-expired branch is unreachable and the
  owner never gets the notification; and `ownerHorizonEnd` is never set on two intake paths, which
  `LiveFeasibilityService.java:72` treats as "no horizon."
- **RULE-10** established that `RetentionService` cannot reach log files, and that because
  `full_ai_response` is never persisted, **the log is the system's only durable copy of raw AI
  output** — making the 30-day `DATA_PURGED` audit record false.

## Cost shape

Tier one: ~14 minutes, serial, 13 rules, produced addresses.
Tier two: 8 agents in parallel, ~4-8 minutes each, produced consequences and a ranking.

The engineering consequence: **triage is serial and cheap; diagnosis is parallel and expensive, and
you only pay for it on findings that survived triage.** The fan-out is affordable precisely because
the judge localised first — no investigator read 166 files, each started at a named line.

## The ranking is the deliverable

The judge emitted findings in rule-number order — RULE-1, 2, 4, 5, 8, 10, 11, 12 — which is document
order and carries no priority. After investigation the order is unrecognisable, and I had personally
ranked RULE-2 last of eight while it contained the single most alarming line in the audit.

An unranked list of eight is a backlog. Ranking requires consequence; consequence requires
reachability; reachability requires reading past the cited line.

## Honest limits

- One codebase, one model, one run per finding. No repeat runs, so nothing here speaks to variance.
- The ground truth was established by the same person who then judged the agents against it, and
  who wrote the prompts. Unblinded throughout.
- Spot-checks covered roughly a dozen claims, not all of them. "0 fabrications detected" is a
  statement about what was checked.
- Severity labels are the agents' own and were not independently calibrated.
