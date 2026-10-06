package io.github.markpollack.judge.tutorial.rules;
import org.junit.jupiter.api.Test;
import io.github.markpollack.judge.tutorial.support.RecordedEvalModel;
import static io.github.markpollack.judge.assertj.Assertions.assertThat;
/** Intentional subject gate, excluded by Surefire's ordinary *Test discovery. */
class ShouldIMergeArchitectureDemo {
    @Test void shouldMerge() {
        assertThat(Rfc2119RulesDemo.jury(new RecordedEvalModel("architecture-rules"))).isPassed();
    }
}
