package io.github.markpollack.judge.tutorial.support;

import java.util.List;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.jury.Jury;
import io.github.markpollack.judge.ai.model.EvalModel;
import io.github.markpollack.judge.ai.requirements.*;
import io.github.markpollack.judge.execution.*;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.requirement.Requirement;

/** Prepared evidence and structured counterparts to the investigative demo factories. */
public final class ConfiguredExamples {
    private ConfiguredExamples() { }
    public static Judge preparedRfc(Requirement<Rfc2119Specification> requirement, EvalModel runtime, String evidence) {
        return Rfc2119Judge.builder().runtime(runtime).requirement(requirement).evidence(evidence).build();
    }
    public static Judge preparedEars(Requirement<EarsSpecification> requirement, EvalModel runtime, String evidence) {
        return EarsJudge.builder().runtime(runtime).requirement(requirement).evidence(evidence).build();
    }
    public static Jury structuredEars(List<EarsRequirement> roster,
            EvalRuntime<RequirementRequest<EarsSpecification, String>, Judgment> runtime, String evidence) {
        return EarsJury.builder().runtime(runtime).requirements(roster).evidence(evidence).build();
    }
    public static Jury structuredRfc(List<Rfc2119Requirement> roster,
            EvalRuntime<RequirementRequest<Rfc2119Specification, String>, Judgment> runtime, String evidence) {
        return Rfc2119Jury.builder().runtime(runtime).requirements(roster).evidence(evidence).build();
    }
    public static Judge structuredSingle(Requirement<Rfc2119Specification> requirement,
            EvalRuntime<RequirementRequest<Rfc2119Specification, String>, Judgment> runtime, String evidence) {
        return Rfc2119Judge.builder().runtime(runtime).requirement(requirement).evidence(evidence).build();
    }
}
