package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatedraKafkaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CatedraKafkaConfiguration.class);

    @Test
    void bindsTheRetryPolicy() {
        contextRunner.withPropertyValues("catedra.kafka.max-retries=3", "catedra.kafka.initial-backoff=1s")
                .run(context -> assertThat(context.getBean(CatedraKafkaProperties.class))
                        .isEqualTo(new CatedraKafkaProperties(3, Duration.ofSeconds(1))));
    }

    @Test
    void acceptsZeroRetries() {
        contextRunner.withPropertyValues("catedra.kafka.max-retries=0", "catedra.kafka.initial-backoff=1s")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void contextFailsWithNegativeRetries() {
        contextRunner.withPropertyValues("catedra.kafka.max-retries=-1", "catedra.kafka.initial-backoff=1s")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("maxRetries"));
    }

    @Test
    void contextFailsWithoutTheInitialBackoff() {
        contextRunner.withPropertyValues("catedra.kafka.max-retries=3")
                .run(context -> assertThat(context).getFailure().hasStackTraceContaining("initialBackoff"));
    }
}
