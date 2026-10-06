package io.github.markpollack.judge.tutorial.ears;

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
class EarsSliceTest {
    @Test
    void actualDemoRetainsRosterNativeOwnershipPolicyAndV6WithoutRepeatedExecution() {
        var calls = new AtomicInteger();
        var policies = new AtomicInteger();
        EvalModel recorded = new RecordedEvalModel("ears-uc6-cancellation");
        EvalModel runtime = request -> { calls.incrementAndGet(); return recorded.generate(request); };
        var jury = EarsSliceDemo.jury(runtime);
        assertThat(calls).hasValue(0);
        var stage = Assertions.assertThat(jury).withPolicy(whole -> {
            policies.incrementAndGet();
            assertThat(whole.roster()).hasSize(6);
            assertThat(whole.compositeAttempts()).hasSize(6);
            return new PolicyDecision(PolicyAction.ESCALATE, "Illustrative application review; keep conclusion");
        });
        var evaluation = stage.evaluate();
        var result = evaluation.verdict();
        assertThat(result.conclusion()).isEqualTo(Verdict.Conclusion.PASS);
        assertThat(result.roster()).hasSize(6);
        assertThat(result.compositeAttempts()).hasSize(6);
        assertThat(result.individual()).isEmpty();
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.PASS).count()).isEqualTo(6L);
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.FAIL).count()).isEqualTo(0L);
        assertThat(RosterResults.checks(result).stream().filter(c -> c.judgment().status() == JudgmentStatus.ABSTAIN).count()).isEqualTo(0L);
        assertThat(InvocationRecords.of(result)).hasSize(1);
        var root = result.invocations().getFirst();
        assertThat(root.nativeFacts()).containsKeys("text", "messages", "answerState", "recording", "provenance");
        assertThat(root.nativeFacts()).doesNotContainKey("usage"); // archived token usage is unknown
        for (int i = 0; i < 6; i++) {
            var requirement = result.roster().get(i);
            var child = result.compositeAttempts().get(i).verdict().individual().getFirst();
            assertThat(child.requirement()).isSameAs(requirement);
            assertThat(requirement.revision()).isEqualTo("petclinic:fc9df4af");
            assertThat(requirement.source().artifact().sha256()).hasSize(64);
            assertThat(child.invocations()).isEmpty();
            assertThat(child.invocationIds()).containsExactly(root.id());
        }
        assertThat(result.roster()).extracting(q -> q.id()).containsExactly("UC6-AC7", "UC6-AC8", "UC6-AC9", "UC6-AC10", "UC6-AC11", "UC6-AC12");
        assertThat(io.github.markpollack.judge.ai.requirements.Observation.of(result.judgment())).isNotEmpty();
        var codec = NativeRequirementCodecs.codec();
        var reopened = codec.readEvaluation(codec.write(evaluation));
        assertThat(reopened).isEqualTo(evaluation);
        assertThat(reopened.verdict().requireUsable()).isSameAs(reopened.verdict());
        Assertions.assertThat(reopened).hasConclusion(Verdict.Conclusion.PASS);
        Assertions.assertThat(reopened.verdict()).hasConclusion(Verdict.Conclusion.PASS);
        assertThat(stage.evaluate()).isSameAs(evaluation);
        assertThatThrownBy(stage::isPassed).isInstanceOf(AssertionError.class); // final policy withholds reliance
        assertThat(calls).hasValue(1);
        assertThat(policies).hasValue(1);
    }
}
