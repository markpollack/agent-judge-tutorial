package io.github.markpollack.judge.tutorial.ears;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.ai.requirements.EarsJudge;
import io.github.markpollack.judge.ai.requirements.EarsRequirement;
import io.github.markpollack.judge.ai.requirements.NativeRequirementCodecs;
import io.github.markpollack.judge.ai.requirements.Rfc2119Judge;
import io.github.markpollack.judge.ai.requirements.Rfc2119Requirement;
import io.github.markpollack.judge.evaluation.PolicyResult;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import io.github.markpollack.judge.policy.PolicyAction;
import io.github.markpollack.judge.policy.PolicyDecision;
import io.github.markpollack.judge.tutorial.support.Candidate;
import io.github.markpollack.judge.tutorial.support.RecordedEvalModel;
import io.github.markpollack.judge.verdict.Verdict;

import static io.github.markpollack.judge.assertj.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Stage examples over real requirements/source and verbatim archived answers; zero inference. */
class RequirementExamplesTest {

    @Test
    void oneRfc2119Requirement() throws IOException {
        var requirement = Rfc2119Requirement.from(Candidate.SPEC.resolve("rules.md"), "petclinic:fc9df4af")
            .stream().filter(q -> q.id().equals("RULE-3")).findFirst().orElseThrow();
        var evidence = source("src/main/java/org/springframework/samples/petclinic/scheduling/model/Reservation.java")
            + source("src/main/java/org/springframework/samples/petclinic/scheduling/model/Appointment.java");
        var runtime = RecordedEvalModel.singleRequirement("architecture-rules", requirement.id());

        var judge = Rfc2119Judge.builder().runtime(runtime)
            .requirement(requirement).evidence(evidence).build();
        var judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertSame(requirement, judgment.requirement());
        assertTrue(judgment.invocations().getFirst().nativeFacts().containsKey("archivedRosterResponse"));
        System.out.println("RULE-3: " + judgment.status() + " (archived roster answer, selected verbatim)");
        System.out.println(judgment.reasoning());
    }

    @Test
    void oneEarsRequirement() throws IOException {
        var requirement = EarsRequirement.from(Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md"),
            "petclinic:fc9df4af").stream().filter(q -> q.id().equals("UC6-AC7")).findFirst().orElseThrow();
        var evidence = cancellationEvidence();
        var runtime = RecordedEvalModel.singleRequirement("ears-uc6-cancellation", requirement.id());

        var judge = EarsJudge.builder().runtime(runtime)
            .requirement(requirement).evidence(evidence).build();
        var judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertSame(requirement, judgment.requirement());
        System.out.println("UC6-AC7: " + judgment.status() + " (archived roster answer, selected verbatim)");
        System.out.println(judgment.reasoning());
    }

    @Test
    void requirementFirstAssertion() throws IOException {
        var requirement = EarsRequirement.from(Candidate.SPEC.resolve("manage-appointment-lifecycle/criteria.md"),
            "petclinic:fc9df4af").stream().filter(q -> q.id().equals("UC6-AC7")).findFirst().orElseThrow();
        var evidence = cancellationEvidence();
        var replay = RecordedEvalModel.singleRequirement("ears-uc6-cancellation", requirement.id());
        var calls = new AtomicInteger();
        EvalModel runtime = request -> { calls.incrementAndGet(); return replay.generate(request); };

        var stage = assertThat(requirement).judgedBy(EarsJudge.builder().runtime(runtime))
            .withEvidence(evidence);
        stage.isSatisfied();
        var complete = stage.evaluate();
        stage.isSatisfied(); // The same stage caches its complete result.

        assertEquals(1, calls.get());
        var codec = NativeRequirementCodecs.codec();
        var reopened = codec.readEvaluation(codec.write(complete));
        assertEquals(complete, reopened);
        assertThat(reopened).hasConclusion(Verdict.Conclusion.PASS);
        assertEquals(1, calls.get());
        System.out.println("Requirement-first: satisfied; cached backend executions=" + calls.get());
    }

    @Test
    void conclusionAndApplicationReliance() {
        var jury = EarsSliceDemo.jury(new RecordedEvalModel("ears-uc6-cancellation"));
        assertThat(jury).isPassed(); // Ready-Jury assertion: the six requirements passed on replay.

        var application = assertThat(jury).withPolicy(wholeVerdict ->
            new PolicyDecision(PolicyAction.ESCALATE, "Illustrative application rule: human review before merge"));
        var complete = application.evaluate();
        var action = ((PolicyResult.Decided) complete.policyResult()).decision().action();

        assertEquals(Verdict.Conclusion.PASS, complete.verdict().conclusion());
        assertEquals(PolicyAction.ESCALATE, action);
        assertThrows(AssertionError.class, application::isPassed);
        assertSame(complete, application.evaluate());
        System.out.println("Conclusion=" + complete.verdict().conclusion() + "; application action=" + action);
        // Full Verdict and policy decision stay in complete; display abbreviations discard nothing.
    }

    private static String cancellationEvidence() throws IOException {
        return source("src/main/java/org/springframework/samples/petclinic/scheduling/service/AppointmentService.java")
            + source("src/test/java/org/springframework/samples/petclinic/scheduling/AppointmentServiceTests.java");
    }

    private static String source(String relativePath) throws IOException {
        return "\nFile: " + relativePath + "\n" + Files.readString(Candidate.workspace().resolve(relativePath));
    }
}
