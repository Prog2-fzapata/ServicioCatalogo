package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraSyncSource;
import lombok.RequiredArgsConstructor;

// Con el cableado real de Spring (las variables CATEDRA_REDIS_* llegan al application.properties) y una cuenta
// como la de la cátedra: lee catedra:sync:* y nada más. Si el adapter intentara escribir, Redis lo rechazaría
// y estos tests fallarían
@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class CatedraSyncSourceIntegrationTest {

    private static final String USER = "grp_prueba";
    private static final String PASSWORD = "clave-de-prueba";

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory adminFactory;

    private final CatedraSyncSource catedraSyncSource;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("CATEDRA_REDIS_HOST", REDIS::getHost);
        registry.add("CATEDRA_REDIS_PORT", () -> REDIS.getMappedPort(6379));
        registry.add("CATEDRA_REDIS_USERNAME", () -> USER);
        registry.add("CATEDRA_REDIS_PASSWORD", () -> PASSWORD);
    }

    @BeforeAll
    static void createReadOnlyUserAndData() throws IOException, InterruptedException {
        // Solo lectura sobre catedra:sync:*, como la ACL de la cuenta técnica (anexo §14.1)
        REDIS.execInContainer("redis-cli", "ACL", "SETUSER", USER, "on", ">" + PASSWORD, "~catedra:sync:*",
                "+@read", "+@connection");

        adminFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        adminFactory.afterPropertiesSet();
        StringRedisTemplate admin = new StringRedisTemplate(adminFactory);
        admin.opsForHash().putAll("catedra:sync:metadata",
                Map.of("currentVersion", "7", "oldestAvailableVersion", "4", "publishedAt", "2026-09-10T13:44:59Z"));
        admin.opsForHash().put("catedra:sync:professional-categories", "1", """
                {"id": 1, "name": "Clinica medica", "description": "Atencion clinica general", "enabled": true,
                 "campoNuevo": "lo agrega la cátedra en una versión futura",
                 "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}""");
    }

    @AfterAll
    static void disconnect() {
        adminFactory.destroy();
    }

    @Test
    void readsTheMetadataWithTheConfiguredCredentials() {
        SyncMetadata metadata = catedraSyncSource.readMetadata();

        assertThat(metadata.currentVersion()).isEqualTo(7);
        assertThat(metadata.oldestAvailableVersion()).isEqualTo(4);
        assertThat(metadata.publishedAt()).isEqualTo(Instant.parse("2026-09-10T13:44:59Z"));
    }

    @Test
    void readsAnEntityIgnoringFieldsThatTheContractDoesNotDefine() {
        List<ProfessionalCategory> categories = catedraSyncSource.readCategories(List.of(1L));

        assertThat(categories).singleElement().satisfies(category -> {
            assertThat(category.getName()).isEqualTo("Clinica medica");
            assertThat(category.isEnabled()).isTrue();
        });
    }

    @Test
    void reportsAVersionOutsideTheRetainedWindowAsAbsent() {
        assertThat(catedraSyncSource.readChanges(3)).isEmpty();
    }
}
