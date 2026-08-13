# Module 05: Cascaded Jury

Tiered evaluation with CascadedJury. Cheap checks run first; expensive checks only if cheap ones pass. Fail fast, save cost.

The demo also shows how to inspect composite execution evidence with
`CompositePaths.flatten(verdict)`. Entries are returned in deterministic preorder and include a
canonical path, configured attempt name, relation, and cascade policy. A successful attempt carries
its aggregate status and named individual judgments; a failed attempt instead carries a stable
failure code.

## Documentation

Full tutorial: https://lab.pollack.ai/docs/agent-judge/tutorial

## Running

```bash
./mvnw exec:java -pl module-05-cascaded-jury
```
