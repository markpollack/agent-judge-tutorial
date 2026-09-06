# Step 1.1: Vendor pinned PetClinic fixtures

## What was vendored

| Fixture | Commit | Files | Java | Size |
|---|---|---|---|---|
| `baseline/` | `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272` | 131 | 49 | 1.9 MB |
| `appointment-scheduling-spec-with-usecases/` | `fc9df4af46171bf7b6146d0477cc68d70e8532ad` | 403 | 222 | 4.2 MB |

Both SHAs were verified present upstream before import. Imported with `git archive`, so no
upstream history and no `.git` directory came along. Apache 2.0 `LICENSE.txt` preserved verbatim
in both. Full detail in `fixtures/petclinic/PROVENANCE.md`.

222 Java sources in the large candidate matches the roadmap's "roughly two hundred files" framing,
so the pin is the intended experiment.

## Layout decision

Design DD-9 offered a choice. Chosen: one full baseline, one full large candidate, and the small
change as `city-search.patch` plus a materialization script. One copy of PetClinic per genuinely
different codebase, rather than three.

`materialize-city-search.sh` copies the baseline and applies the patch. It is deterministic,
offline, and never modifies the baseline.

## The city-search change

Written to follow existing conventions, because modules 03 and 04 ask whether it does and the
answer has to be checkable by eye on stage.

The existing owner search is `OwnerRepository.findByLastNameStartingWith(String, Pageable)` feeding
`OwnerController.processFindForm`, which strips input, treats null as the broadest search, redirects
on a single hit, rejects the field with code `notFound` on an empty result, and otherwise delegates
to `addPaginationModel`. The patch mirrors that shape exactly for city and reuses
`addPaginationModel` rather than duplicating pagination.

3 files, 66 added lines, 0 removed. Test count goes 71 to 74.

## Findings worth carrying forward

1. **The upstream build enforces `spring-javaformat`.** My first compile failed on formatting
   before any test ran. This is a real deterministic convention gate living inside the fixture, and
   it is a good module 01 observation: some conventions are already machine-checked, and those do
   not need a judgment oracle.

2. **My own `.gitignore` silently dropped vendored content.** The rule `plans/` was unanchored and
   matched `.junie/plans/` inside the large candidate. Fixed to `/plans/*` with
   `!/plans/learnings/`. Two lessons: anchor ignore rules meant for the repository root, and
   compare the on-disk file count against what git would actually add rather than trusting that a
   copy was complete. The count caught it (533 versus 534).

3. **Negation under an excluded directory does not work.** `/plans/` plus `!/plans/learnings/`
   silently kept ignoring the learnings, because git does not descend into an excluded directory.
   `/plans/*` is required for the negation to be reachable. Verified in both directions afterwards
   rather than assumed.

4. **The candidate builds and tests fully offline** with `mvnw -o test` once the local Maven
   repository is warm. Stronger than the exit criterion, which only required no GitHub access.

## Verification performed

| Check | Result |
|---|---|
| Both upstream SHAs exist | Yes |
| Licenses preserved | Yes, Apache 2.0 in both |
| Baseline compiles | Yes |
| Materialized candidate compiles and tests | 74 tests, 0 failures, 2 skipped |
| Candidate offline build | Yes, `mvnw -o test` green |
| Baseline unmodified by materialization | Yes, 0 occurrences of the new method |
| Files on disk versus files git would add | 534 versus 534 after the ignore fix |

## Deferred

Whether the large candidate's own test suite runs fast enough for the live path is still open. It
is a Step 3.0 question and nothing before then depends on it.
