# PetClinic fixture provenance

Everything under `fixtures/petclinic/` is vendored. Nothing is fetched at run time, so the
tutorial cannot break because an upstream branch moved, was rewritten, or went away.

## Sources

| Fixture | Upstream | Ref | Exact commit | Upstream date |
|---|---|---|---|---|
| `baseline/` | `antonarhipov/spring-petclinic-fork` | `main` | `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272` | 2026-07-30 |
| `appointment-scheduling-spec-with-usecases/` | `antonarhipov/spring-petclinic-fork` | `appointment-scheduling-spec-with-usecases` | `fc9df4af46171bf7b6146d0477cc68d70e8532ad` | 2026-08-27 |

Imported 2026-09-06 by `git archive` of each exact commit, so no upstream git history is
carried and no `.git` directory is vendored.

## License

Both trees are Apache License 2.0. `LICENSE.txt` is preserved verbatim in each, along with
upstream `NOTICE`/readme content. No upstream file was relicensed, and no copyright header was
removed or altered.

Spring PetClinic is a Spring community sample. The `appointment-scheduling-spec-with-usecases`
branch is Anton Arhipov's spec-driven-development experiment and carries its own `spec/`
directory, which is vendored with it because the specification is the evaluation oracle for
module 06.

## Local transformations

| Fixture | Transformation |
|---|---|
| `baseline/` | None. Byte-identical to the upstream commit, minus git metadata |
| `appointment-scheduling-spec-with-usecases/` | None. Byte-identical to the upstream commit, minus git metadata |
| `city-search.patch` | Written for this tutorial. Not upstream |

## The small candidate

The city-search change is stored as a patch over the baseline rather than as a third full tree,
so the repository carries one copy of PetClinic per genuinely different codebase.

Task given, as it would be given to an agent:

> Add a way to find owners by their city. Follow the conventions already used in the codebase.

Materialize it deterministically and offline:

```bash
./materialize-city-search.sh                 # writes build/city-search-candidate
./materialize-city-search.sh /tmp/elsewhere  # or anywhere you like
```

The script copies `baseline/` and applies `city-search.patch`. The baseline is never modified.

### What the patch contains

4 files, 85 added lines, 0 removed:

| File | Change |
|---|---|
| `owner/OwnerRepository.java` | `Page<Owner> findByCityStartingWith(String, Pageable)`, mirroring `findByLastNameStartingWith` |
| `owner/OwnerController.java` | `@GetMapping("/owners/by-city")` mirroring `processFindForm`, plus `findPaginatedForOwnersCity`, reusing the existing `addPaginationModel` |
| `owner/OwnerControllerTests.java` | 3 tests mirroring the existing find-form tests |
| `templates/owners/findOwners.html` | A city search form mirroring the existing last-name form, so the endpoint is reachable the way the convention reaches its own |

It deliberately follows the existing owner-search conventions rather than inventing a new
pattern. Modules 03 and 04 ask whether it does, and the answer needs to be independently
checkable by eye during a live demo. A deliberately non-conforming variant is seeded and
reverted in module 04 rather than baked into this patch.

### Verified state

| Check | Result |
|---|---|
| Baseline compiles | Yes |
| Candidate compiles | Yes |
| Candidate test suite | 74 tests, 0 failures, 2 skipped (MySQL integration, skipped upstream too) |
| Candidate builds fully offline (`mvnw -o test`) | Yes |
| Baseline test count before the change | 71 |

The upstream project enforces `spring-javaformat` at build time, so the patch is formatted to
that standard. A formatting violation fails the build before any test runs, which is itself a
deterministic convention gate worth noticing in module 01.

## Large candidate shape

| | |
|---|---|
| Files | 403 |
| Java sources | 222 |
| Size | 4.2 MB |
| Spec | `spec/smart-appointment-scheduling/` with 7 use-case directories, each carrying `spec.md`, `rules.md`, and `criteria.md`, plus `proposal.md`, `rules.md`, `spec.md`, `status.md`, and `review.md` |

`review.md` is upstream spec-review evidence. It is not implementation-conformance evidence and
must not be used as such by module 06.

### It does not build as delivered

The vendored tree fails `spring-javaformat:validate`, so it is kept exactly as generated and a
buildable copy is derived instead:

```bash
./materialize-large-candidate.sh        # writes build/large-candidate
```

The script applies the formatter and reports which files it had to change, rather than repairing
its subject silently. After that, 290 tests pass with 4 skipped, in about 37 seconds.

The violation is worth stating precisely, because "fails formatting" undersells it. One file of
166, `security/AccountBootstrapRunner.java`, uses the cuddled `} else {` that Spring's house style
forbids, plus non-conforming continuation indents. Seventeen other files use Spring's
`}` newline `else` correctly, and the offending file is an unremarkable 163 lines, neither the
largest nor obviously special.

So the generated system knew a non-obvious house style and applied it correctly seventeen times,
then slipped once. That is not a systematic failure, it is a single lapse in 166 files, and it is
exactly the class of defect a human reviewer misses and a deterministic formatter finds in
thirteen seconds with certainty.

## Re-pinning

Do not silently follow the upstream branch. To move to a newer commit, update the table above,
re-import with `git archive`, re-run the verification, and record why the pin moved.
