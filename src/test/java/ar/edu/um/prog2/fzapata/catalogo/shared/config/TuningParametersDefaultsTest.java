package ar.edu.um.prog2.fzapata.catalogo.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import lombok.RequiredArgsConstructor;

// Los parámetros de ajuste tienen valor por defecto en application.properties y, si la variable de entorno está
// definida, esa gana. Se prueba con el cableado real: el mapeo de la variable a la propiedad
class TuningParametersDefaultsTest {

    @SpringBootTest
    @Import(TestcontainersConfiguration.class)
    @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
    @RequiredArgsConstructor
    @Nested
    class WithoutEnvironment {

        private final CatedraSyncProperties syncProperties;
        private final CatedraKafkaProperties kafkaProperties;
        private final Environment environment;

        @Test
        void usesTheDefaultsOfTheApplicationProperties() {
            assertThat(environment.getProperty("spring.data.web.pageable.max-page-size")).isEqualTo("100");
            assertThat(syncProperties.checkInterval()).isEqualTo(Duration.ofMinutes(5));
            assertThat(syncProperties.initialDelay()).isEqualTo(Duration.ofSeconds(10));
            assertThat(kafkaProperties.maxRetries()).isEqualTo(3);
            assertThat(kafkaProperties.initialBackoff()).isEqualTo(Duration.ofSeconds(1));
        }
    }

    // Las variables de entorno llegan a Spring como propiedades con su mismo nombre: así se simula el entorno
    @SpringBootTest(properties = {
            "CATEDRA_SYNC_CHECK_INTERVAL=2m", "CATEDRA_SYNC_INITIAL_DELAY=3s",
            "CATEDRA_KAFKA_MAX_RETRIES=7", "CATEDRA_KAFKA_INITIAL_BACKOFF=250ms", "CATALOGO_PAGE_MAX_SIZE=50"})
    @Import(TestcontainersConfiguration.class)
    @TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
    @RequiredArgsConstructor
    @Nested
    class WithEnvironment {

        private final CatedraSyncProperties syncProperties;
        private final CatedraKafkaProperties kafkaProperties;
        private final Environment environment;

        @Test
        void theEnvironmentWinsOverTheDefaults() {
            assertThat(environment.getProperty("spring.data.web.pageable.max-page-size")).isEqualTo("50");
            assertThat(syncProperties.checkInterval()).isEqualTo(Duration.ofMinutes(2));
            assertThat(syncProperties.initialDelay()).isEqualTo(Duration.ofSeconds(3));
            assertThat(kafkaProperties.maxRetries()).isEqualTo(7);
            assertThat(kafkaProperties.initialBackoff()).isEqualTo(Duration.ofMillis(250));
        }
    }
}
