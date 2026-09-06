# Step 2.1: Module 03, the first AI architectural judge

## What it is

A tutorial-local `ArchitecturalConformanceJudge`: an ordinary `ModelBackedJudge` with a real prompt
template, a classifier that keeps one `Check` per named criterion, and `AgentClientJudgeModel` as
its backend. One narrow question:

> Does this change follow the conventions the surrounding owner-search code already uses?

Evidence is curated and supplied: the existing `OwnerController` and `OwnerRepository`, plus the
85 line diff. Module 04 stops supplying evidence and lets the judge look for it.

## The judge found two real defects in my own fixture

This is the finding that matters most, and I did not plan it.

**First run, first defect.** FAIL, because the new repository Javadoc had been inserted between
`findById`'s Javadoc and `findById` itself, orphaning the existing documentation and leaving
`findById` undocumented. I wrote that patch and did not notice. I checked the file by hand and the
judge was exactly right.

**After fixing that, second defect.** Four criteria passed and one failed: the change added a
controller endpoint and a repository method but never added the view half, so the new endpoint was
unreachable through the form the existing convention reaches its own search with. I had decided
earlier to skip the HTML to keep the patch small. The judge called it, correctly, as a conformance
gap.

The fixture is now genuinely conventional: 4 files, 85 added lines, including a city form in
`findOwners.html` that mirrors the existing last-name form, using the `city` message key that
already exists.

**An AI judge improved a human-written change twice before it was ever used as a teaching example.**
That is a better argument for the judgment oracle than anything the prose could claim.

## The judge is self-inconsistent, and this is measured

Eight live runs on the same evidence.

| Fixture state | Verdicts |
|---|---|
| Original patch, Javadoc defect | FAIL |
| Javadoc fixed, no view | PASS, then FAIL, FAIL, FAIL |
| Complete and conventional | FAIL, FAIL, PASS, PASS |

On the final, genuinely conventional change the judge disagrees with itself roughly one run in
three. Every individual verdict was defensible and the FAILs cited real, checkable things. There is
no run where it hallucinated.

Three consequences, all of which the tutorial should carry:

1. **Recorded is the right default.** A live verdict that flips one run in three is not something to
   put on a conference stage as the module's punchline. Recorded mode replays a verbatim captured
   run, so the demo is stable and the live path stays one environment variable away.
2. **This is the strongest possible motivation for the jury module.** Module 09 aggregates several
   independent estimates of the same uncertain property. Here is a real property, genuinely
   uncertain, with a measured disagreement rate. That module now has evidence behind it rather than
   a hypothetical.
3. **A single judgment oracle on a borderline case is an estimate, not a fact.** Modules 01 and 02
   produce facts. This one produces an estimate, and the tutorial should not blur them.

## Narrowing the question mattered, and not the way I expected

The first version asked whether the change follows conventions **and** avoids unnecessary
complexity. Those are two questions, and the second is genuinely contested: this change mirrors an
existing search flow, so a reviewer can call it faithful conformance or call it duplication that
should have been factored. One run said each.

The design contract says to ask about conformance to existing owner-search conventions, so the
prompt now says so and explicitly tells the judge that faithful duplication is conformance and that
whether the codebase should factor its two search flows together is a different question.

That reduced the ambiguity but did not remove the inconsistency, which is the honest result.

## Findings

1. **A judge must check `metadata.successful`.** The classifier returns ERROR when the agent run
   failed, rather than parsing a failed run's text into a verdict. Without this an infrastructure
   failure becomes a finding about the code.
2. **A verdict with no criteria is an opinion.** The classifier returns ERROR if the reply states
   VERDICT but names no criteria, so an unparseable answer cannot arrive as a bare PASS.
3. **Models ignore output format instructions.** One run wrapped criterion names in
   `<criterion>...</criterion>` despite the format being specified. The classifier now strips markup
   and surrounding punctuation from criterion names. A free-text contract needs a tolerant parser.
4. **Capture is part of the architecture.** `AGENT_JUDGE_TUTORIAL_CAPTURE=<name>` alongside `=live`
   rewrites the recording from a real run, so recordings are refreshable evidence rather than
   hand-written fixtures. The captured file is verbatim and carries a dated provenance header.

## Verification

| Exit criterion | Result |
|---|---|
| Real demo path uses AgentClient | Yes, 8 live runs |
| CI path validates semantics without provider calls | Yes, recorded, deterministic, 1.2s |
| Question cannot be a trivial deterministic assertion | Yes, and the two defects it found were not greppable |
| Output names evidence-based criteria and a verdict | Yes, 5 criteria, each with a sentence of evidence |
| Output fits 80 columns | Yes, 0 over |
| Integration test | Passes, 7 of 7 required strings |
