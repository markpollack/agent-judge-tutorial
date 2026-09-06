# Step 2.2: Module 04, ground the judgment in investigation

## What changed from module 03

The prompt, and nothing else. Same classifier, same backend selection, same `Judgment`, same
`Check`s. `ArchitecturalConformanceJudge.judge(...)` is now the shared assembly point and module 04
supplies a different template, so this is one concept at a later stage rather than a second judge.

Module 03 handed over two files and a diff. Module 04 hands over a workspace and asks the judge to
find the convention itself, report the population it examined, and name what departs from it.

## The result that justifies the module

Module 03, given curated evidence, said **PASS**.
Module 04, allowed to look at the whole codebase, said **FAIL**, twice, for a reason the curated
evidence could not have contained:

> `ClinicServiceTests` covers every pre-existing custom finder but adds no case for
> `findByCityStartingWith`, leaving the new query the only repository finder never executed against
> the database. `OwnerControllerTests` adds three city tests against five for last name, omitting
> the whitespace tests even though the handler copies the `strip()` behaviour those tests exist to
> pin.

Both claims are cited with file and line, and both are true. **The curated evidence set never
included the test files, so the human who chose the evidence had already decided the answer.** That
is the module's entire argument, and it arrived as an observed result rather than a claim in prose.

This was not designed. I chose the module 03 evidence set by hand and it was incomplete in a way I
did not notice, which is exactly the failure mode the module describes.

## The seeded exception was detected precisely

The demo seeds one departure into the city handler: it stops calling the shared
`addPaginationModel` and inlines its own model attributes. The judge found it and explained it
better than the seed was written:

```
FAIL  pagination-model-helper
      OwnerController.java:151-154 inlines three model.addAttribute calls instead of
      returning addPaginationModel(...), the private helper that both pre-existing
      paginated handlers use (OwnerController.java:121 and VetController.java:47),
      making it the only paginated handler in the codebase that populates the model itself.

FAIL  model-attribute-names
      invents attribute names the reused ownersList view does not read
```

The second finding is a consequence of the seed I had not thought about. It also correctly left the
last-name handler alone, which is what makes it a conformance finding rather than a diff summary.

The seed is applied before the run and reverted in a `finally` block. Verified reverted.

## Two bugs found while building it

1. **The seed silently did not apply.** PetClinic indents with tabs and my hard-coded snippet used
   spaces, so the string never matched and the "seeded" run judged unmodified code. It was caught
   because the exception message said so, but only by luck of the guard existing. The seed now
   derives its indentation from the file, replaces only inside the city handler, and **asserts that
   something changed** before spending a live run. A seeded-defect test that does not verify the
   seed landed proves nothing.

2. **Recordings for both modules live in module 03's resources.** Deliberate: module 03 owns
   `RecordedJudgeModel` and the judge machinery, and module 04 depends on it, so one judge concept
   keeps one recordings directory. Worth revisiting at Step 5.0 if it reads as untidy.

## Cost, and what it means for the live path

| Run | Live | Recorded |
|---|---|---|
| Module 04, both judgments | 105s + 63s | 2.2s |

Nearly three minutes of live investigation is too slow to run on stage. Recorded replay is the
default and is what the conference path uses; the live path stays one environment variable away and
is what makes the recordings real.

## Verification

| Exit criterion | Result |
|---|---|
| The judge discovers evidence rather than consuming preselected files | Yes, and it found what the preselected set omitted |
| Output makes its evidence inspectable | Yes, pattern, population, and file-and-line citations |
| A seeded exception is detected, then reverted | Yes, two findings on the seed; fixture verified clean afterwards |
| Output fits 80 columns | Yes, 0 over 101 lines |
| Integration test | Passes, 8 of 8 required strings |
