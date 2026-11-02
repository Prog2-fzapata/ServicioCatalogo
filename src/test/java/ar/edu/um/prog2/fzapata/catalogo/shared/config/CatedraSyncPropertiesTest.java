package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatedraSyncPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CatedraSyncConfiguration.class);

    @Test
    void bindsTheIntervalAndTheInitialDelayInSimpleDurationFormat() {
        contextRunner.withPropertyValues("catedra.sync.check-interval=5m", "catedra.sync.initial-delay=10s")
                .run(context -> assertThat(context.getBean(CatedraSyncProperties.class))
                        .isEqualTo(new CatedraSyncProperties(Duration.ofMinutes(5), Duration.ofSeconds(10))));
    }

    @Test
    void acceptsNoInitialDelay() {
        contextRunner.withPropertyValues("catedra.sync.check-interval=1m", "catedra.sync.initial-delay=0s")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void contextFailsWithAZeroInterval() {
        contextRunner.withPropertyValues("catedra.sync.check-interval=0s", "catedra.sync.initial-delay=10s")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("checkInterval"));
    }

    @Test
    void contextFailsWithANegativeInitialDelay() {
        contextRunner.withPropertyValues("catedra.sync.check-interval=1m", "catedra.sync.initial-delay=-1s")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("initialDelay"));
    }

    @Test
    void contextFailsWhenAnyValueIsMissing() {
        contextRunner.withPropertyValues("catedra.sync.check-interval=1m")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("initialDelay"));
        contextRunner.withPropertyValues("catedra.sync.initial-delay=10s")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("checkInterval"));
    }
}
