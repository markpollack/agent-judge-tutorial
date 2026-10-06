package io.github.markpollack.judge.tutorial.support;

import java.util.List;
import io.github.markpollack.judge.judgment.Check;
import io.github.markpollack.judge.verdict.Verdict;

/** Display-only views. Always retain the whole Verdict for storage, policy and assertions. */
public final class RosterResults {
    private RosterResults() { }
    public static List<Check> checks(Verdict verdict) {
        return verdict.compositeAttempts().stream().map(a ->
            new Check(a.name(), a.verdict().individual().getFirst())).toList();
    }
}
