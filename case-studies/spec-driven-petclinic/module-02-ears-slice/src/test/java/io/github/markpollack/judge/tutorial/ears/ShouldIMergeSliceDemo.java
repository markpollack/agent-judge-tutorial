package io.github.markpollack.judge.tutorial.ears;
import org.junit.jupiter.api.Test;
import io.github.markpollack.judge.tutorial.support.RecordedEvalModel;
import static io.github.markpollack.judge.assertj.Assertions.assertThat;
/** Intentional subject gate, excluded by Surefire's ordinary *Test discovery. */
class ShouldIMergeSliceDemo {
    @Test void shouldMerge() {
        assertThat(EarsSliceDemo.jury(new RecordedEvalModel("ears-uc6-cancellation"))).isPassed();
    }
}
