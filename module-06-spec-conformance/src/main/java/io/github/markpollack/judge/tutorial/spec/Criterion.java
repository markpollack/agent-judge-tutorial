package io.github.markpollack.judge.tutorial.spec;

/**
 * One written requirement a judge has to answer, with a stable identifier.
 *
 * <p>The identifier is the point. It is what makes an answer traceable back to the thing that was
 * asked, what lets the roster guard notice a missing answer, and what stops a judge from quietly
 * re-carving the question set into one it prefers. Module 03 had to invent its criteria and was
 * self-inconsistent until they were fixed; everything here arrives numbered by somebody else.
 *
 * <p>Two very different documents implement this: a use case's acceptance criteria, and the
 * feature's architectural rules. Both are lists of numbered requirements, so both reach the same
 * judge. What differs is how answerable they are, which is module 08's subject.
 */
public interface Criterion {

    /** Stable identifier from the source document, such as {@code UC6-AC5} or {@code RULE-2}. */
    String id();

    /** Short human label for terminal output. */
    String title();

    /** The requirement as the judge is asked to assess it. */
    String asPrompt();
}
