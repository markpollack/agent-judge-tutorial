package io.github.markpollack.judge.tutorial.support;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import io.github.markpollack.judge.ai.model.*;
import io.github.markpollack.judge.ai.requirements.*;
import io.github.markpollack.judge.execution.*;
import io.github.markpollack.judge.judgment.*;
import io.github.markpollack.judge.verdict.*;
import io.github.markpollack.judge.evaluation.*;
import io.github.markpollack.judge.provenance.Invocation;
import io.github.markpollack.judge.assertj.Assertions;
import static org.assertj.core.api.Assertions.*;

/** Public caller conformance with deterministic stubs, never live model evaluation. */
class EarsJudgeTest {
    @Test void structuredRostersCountEachActualNativeExecution() {
        var ears = EarsRequirement.from(Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md"), "petclinic:fc9df4af");
        var six = ears.stream().filter(q -> Set.of("UC6-AC7", "UC6-AC8", "UC6-AC9", "UC6-AC10", "UC6-AC11", "UC6-AC12").contains(q.id())).toList();
        assertThat(ears).hasSize(52);
        assertThat(six).hasSize(6);
        for (var roster : List.of(six, ears)) {
            var calls = new AtomicInteger();
            EvalRuntime<RequirementRequest<EarsSpecification, String>, Judgment> runtime = q -> {
                calls.incrementAndGet();
                assertThat(q.evidence()).isEqualTo("compiled facts");
                return new NativeExecution<>(Judgment.pass("deterministic stub").forRequirement(q.requirement()), invocation(q.requirement().id()));
            };
            var result = ConfiguredExamples.structuredEars(roster, runtime, "compiled facts").vote();
            assertThat(calls).hasValue(roster.size());
            assertThat(InvocationRecords.of(result)).hasSize(roster.size());
            assertThat(result.roster()).containsExactlyElementsOf(roster);
            assertThat(NativeRequirementCodecs.codec().read(NativeRequirementCodecs.codec().write(result))).isEqualTo(result);
        }
        var rfc = Rfc2119Requirement.from(Candidate.SPEC.resolve("rules.md"), "petclinic:fc9df4af");
        var calls = new AtomicInteger();
        EvalRuntime<RequirementRequest<Rfc2119Specification, String>, Judgment> runtime = q -> {
            calls.incrementAndGet();
            assertThat(q.evidence()).isEqualTo("compiled facts");
            return new NativeExecution<>(Judgment.pass("stub").forRequirement(q.requirement()), invocation(q.requirement().id()));
        };
        var result = ConfiguredExamples.structuredRfc(rfc, runtime, "compiled facts").vote();
        assertThat(rfc).hasSize(13);
        assertThat(calls).hasValue(13);
        assertThat(InvocationRecords.of(result)).hasSize(13);
        assertThat(NativeRequirementCodecs.codec().read(NativeRequirementCodecs.codec().write(result))).isEqualTo(result);
    }
    @Test void preparedSingleAndRequirementFirstAssertJUseActualInputsAndRetainUsage() {
        var q = Rfc2119Requirement.from(Candidate.SPEC.resolve("rules.md"), "petclinic:fc9df4af").getFirst();
        var calls = new AtomicInteger();
        EvalModel runtime = request -> {
            calls.incrementAndGet();
            assertThat(request.messages().getFirst().content()).contains(q.id(), q.specification().requirement(), "captured source facts");
            return new EvalModelResponse(q.id()+": PASS - Stub.java:1", "stub", Usage.builder().inputTokens(11).outputTokens(7).build(), Map.of("sessionId", "stub-session"));
        };
        var direct = ConfiguredExamples.preparedRfc(q, runtime, "captured source facts").judge();
        assertThat(direct.requirement()).isSameAs(q);
        assertThat(direct.invocations().getFirst().nativeFacts()).containsKeys("usage", "sessionId", "text");
        var stage = Assertions.assertThat(q).judgedBy(Rfc2119Judge.builder().runtime(runtime)).withEvidence("captured source facts");
        stage.isSatisfied();stage.isSatisfied();
        assertThat(calls).hasValue(2);
        var ears = EarsRequirement.from(Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md"), "petclinic:fc9df4af").getFirst();
        var e = ConfiguredExamples.preparedEars(ears, request -> new EvalModelResponse(ears.id()+": PASS - Stub.java:2", "stub", null, Map.of()), "captured evidence").judge();
        assertThat(e.requirement()).isSameAs(ears);
        Assertions.assertThat(Evaluations.evaluate(() -> e)).isPassed();
    }
    @Test void completeRefusedOriginalOwnChecksAndLowerInvocationSurviveCachedAssertionAndCodec() {
        var q = Rfc2119Requirement.from(Candidate.SPEC.resolve("rules.md"), "petclinic:fc9df4af").getFirst();
        var alien = Rfc2119Requirement.of("ALIEN", "2", "MUST", "different property", "different source", null);
        var original = Judgment.pass("wrong association").forRequirement(alien).withInvocation(invocation("lower")).toBuilder().check(Check.pass("own-check", "native fact")).build();
        var calls = new AtomicInteger();
        EvalRuntime<RequirementRequest<Rfc2119Specification, String>, Judgment> runtime = request -> {
            calls.incrementAndGet();
            assertThat(request.requirement()).isSameAs(q);
            assertThat(request.evidence()).isEqualTo("prepared facts");
            return new NativeExecution<>(original, invocation("item"));
        };
        var judge = ConfiguredExamples.structuredSingle(q, runtime, "prepared facts");
        var stage = Assertions.assertThat(judge);
        var result = stage.evaluate();
        assertThat(result.verdict().conclusion()).isEqualTo(Verdict.Conclusion.INCONCLUSIVE);
        assertThat(result.verdict().individual().getFirst()).isSameAs(original);
        var seat = result.verdict().seats().getFirst();
        assertThat(seat.execution()).isEqualTo(SeatExecution.RETURNED_REJECTED);
        assertThat(seat.cause()).isNull();
        assertThat(seat.rejection().refusedReturn().original()).isSameAs(original);
        assertThat(seat.rejection().refusedReturn().expected()).isSameAs(q);
        assertThat(original.checks()).containsExactly(Check.pass("own-check", "native fact"));
        assertThat(InvocationRecords.of(result.verdict())).extracting(Invocation::id).containsExactlyInAnyOrder("lower", "item");
        var codec = NativeRequirementCodecs.codec();
        var reopened = codec.readEvaluation(codec.write(result));
        assertThat(reopened).isEqualTo(result);
        assertThatThrownBy(() -> Assertions.assertThat(reopened).isPassed()).isInstanceOf(AssertionError.class);
        assertThatThrownBy(stage::isPassed).isInstanceOf(AssertionError.class);
        assertThat(stage.evaluate()).isSameAs(result);
        assertThat(calls).hasValue(1);
    }
    @Test void malformedOrIncompleteRosterCannotPassAndObservationsStayOutsideRoster() {
        var a = EarsRequirement.of("A", "1", "first", "The system shall retain data", null);
        var b = EarsRequirement.of("B", "1", "second", "The system shall preserve data", null);
        for (String answer : List.of("A: PASS - fact", "A: PASS - fact\nA: FAIL - duplicate\nB: PASS - fact", "A: PASS - fact\nB: NOT_APPLICABLE - no evidence", "unparseable")) {
            var result = EarsJury.builder().runtime((EvalModel) r -> new EvalModelResponse(answer, "stub", null, Map.of())).requirements(List.of(a,b)).build().vote();
            assertThat(result.conclusion()).isEqualTo(Verdict.Conclusion.INCONCLUSIVE);
            assertThat(result.compositeAttempts()).hasSize(2);
            assertThatThrownBy(() -> Assertions.assertThat(result).isPassed()).isInstanceOf(AssertionError.class);
        }
        var result = EarsJury.builder().runtime((EvalModel) r -> new EvalModelResponse("A: PASS - fact\nB: CANNOT_DETERMINE - missing\nOBSERVATION A: a test gap at Foo.java:10", "stub", null, Map.of())).requirements(List.of(a,b)).build().vote();
        assertThat(result.roster()).containsExactly(a,b);
        assertThat(result.conclusion()).isEqualTo(Verdict.Conclusion.INCONCLUSIVE);
        assertThat(Observation.of(result.judgment())).hasSize(1);
    }
    @Test void cancellationAndUnsupportedInputFailWithoutPromotingSubjectOutcome() {
        var q = Rfc2119Requirement.of("A", "1", "MUST", "preserve", "audit", null);
        EvalModel cancelled = request -> { throw new CancellationException("caller cancelled"); };
        var stage = Assertions.assertThat(ConfiguredExamples.preparedRfc(q, cancelled, "facts"));
        assertThatThrownBy(stage::evaluate).isInstanceOf(CancellationException.class);
        assertThatThrownBy(stage::evaluate).isInstanceOf(CancellationException.class);
        var calls = new AtomicInteger();
        EvalModel prepared = ((EvalModel) request -> {calls.incrementAndGet(); return new EvalModelResponse("A: PASS - fact", "stub", null, Map.of());}).withInputs(GeneratedInput.PREPARED_EVIDENCE);
        assertThatThrownBy(() -> Rfc2119Jury.builder().runtime(prepared).requirements(List.of(q)).build()).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).hasValue(0);
    }
    @Test void displayingWrapperPreservesBackendCapabilitiesAndNativeExecution() {
        var q = Rfc2119Requirement.from(Candidate.SPEC.resolve("rules.md"), "petclinic:fc9df4af").getFirst();
        var calls = new AtomicInteger();
        var nativeAnswer = new NativeExecution<>(new EvalModelResponse(q.id()+": PASS - Stub.java:1", "stub", null, Map.of()), invocation("wrapped-native"));
        EvalModel backend = new EvalModel() {
            @Override public Set<GeneratedInput> supportedInputs() { return Set.of(GeneratedInput.PREPARED_EVIDENCE); }
            @Override public EvalModelResponse generate(EvalModelRequest request) { throw new AssertionError("must preserve native execute"); }
            @Override public NativeExecution<EvalModelResponse> execute(EvalModelRequest request) {
                calls.incrementAndGet();
                return nativeAnswer;
            }
        };
        var showing = JudgeBackends.showing(backend);
        assertThat(showing.supportedInputs()).isEqualTo(backend.supportedInputs());
        assertThat(showing.execute(EvalModelRequest.user("display"))).isSameAs(nativeAnswer);
        var result = ConfiguredExamples.preparedRfc(q, showing, "facts").judge();
        assertThat(result.invocations()).containsExactly(nativeAnswer.invocation());
        assertThat(result.requirement()).isSameAs(q);
        assertThat(calls).hasValue(2);
        assertThatThrownBy(() -> Rfc2119Jury.builder().runtime(showing).requirements(List.of(q)).build()).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).hasValue(2);
    }
    private static Invocation invocation(String id) {
        return new Invocation(id, "tutorial-stub:v1", true, "stub", 0, Map.of("nativeAnswer", "retained:"+id), List.of());
    }
}
