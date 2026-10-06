package io.github.markpollack.judge.tutorial.rules;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.ai.requirements.NativeRequirementCodecs;
import io.github.markpollack.judge.assertj.Assertions;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import io.github.markpollack.judge.verdict.*;
import io.github.markpollack.judge.policy.*;
import io.github.markpollack.judge.tutorial.support.*;
import static org.assertj.core.api.Assertions.*;

/** Fresh execution of the migrated demo caller; model text comes from an archived recording. */
class Rfc2119RulesTest {
    @Test
    void actualDemoRetainsRosterNativeOwnershipPolicyAndV6WithoutRepeatedExecution() {
        var calls = new AtomicInteger();
        var policies = new AtomicInteger();
        EvalModel recorded = new RecordedEvalModel("architecture-rules");
        EvalModel runtime = request -> { calls.incrementAndGet(); return recorded.generate(request); };
        var jury = Rfc2119RulesDemo.jury(runtime);
        assertThat(calls).hasValue(0);
        var stage = Assertions.assertThat(jury).withPolicy(whole -> {
            policies.incrementAndGet();
            assertThat(whole.roster()).hasSize(13);
            assertThat(whole.compositeAttempts()).hasSize(13);
            return new PolicyDecision(PolicyAction.ESCALATE, "Illustrative application review; keep conclusion");
        });
        var evaluation = stage.evaluate();
        var result = evaluation.verdict();
        assertThat(result.conclusion()).isEqualTo(Verdict.Conclusion.FAIL);
        assertThat(result.roster()).hasSize(13);
        assertThat(result.compositeAttempts()).hasSize(13);
        assertThat(result.individual()).isEmpty();
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.PASS).count()).isEqualTo(5L);
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.FAIL).count()).isEqualTo(8L);
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.ABSTAIN).count()).isEqualTo(0L);
        assertThat(InvocationRecords.of(result)).hasSize(1);
        var root = result.invocations().getFirst();
        assertThat(root.nativeFacts()).containsKeys("text", "messages", "answerState", "recording", "provenance");
        assertThat(root.nativeFacts()).doesNotContainKey("usage"); // archived token usage is unknown
        for (int i = 0; i < 13; i++) {
            var requirement = result.roster().get(i);
            var child = result.compositeAttempts().get(i).verdict().individual().getFirst();
            assertThat(child.requirement()).isSameAs(requirement);
            assertThat(requirement.revision()).isEqualTo("petclinic:fc9df4af");
            assertThat(requirement.source().artifact().sha256()).hasSize(64);
            assertThat(child.invocations()).isEmpty();
            assertThat(child.invocationIds()).containsExactly(root.id());
        }
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.FAIL).map(c -> c.id()).toList()).containsExactly("RULE-1", "RULE-2", "RULE-4", "RULE-5", "RULE-8", "RULE-10", "RULE-11", "RULE-12");
        var codec = NativeRequirementCodecs.codec();
        var reopened = codec.readEvaluation(codec.write(evaluation));
        assertThat(reopened).isEqualTo(evaluation);
        assertThat(reopened.verdict().requireUsable()).isSameAs(reopened.verdict());
        Assertions.assertThat(reopened).hasConclusion(Verdict.Conclusion.FAIL);
        Assertions.assertThat(reopened.verdict()).hasConclusion(Verdict.Conclusion.FAIL);
        assertThat(stage.evaluate()).isSameAs(evaluation);
        assertThatThrownBy(stage::isPassed).isInstanceOf(AssertionError.class); // final policy withholds reliance
        assertThat(calls).hasValue(1);
        assertThat(policies).hasValue(1);
    }
}
