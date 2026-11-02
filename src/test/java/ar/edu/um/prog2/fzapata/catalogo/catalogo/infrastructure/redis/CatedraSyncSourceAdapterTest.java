package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraResponseException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.Professional;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProfessionalCategory;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.WeeklySchedule;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.CatedraEntityMapper;
import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraContractFixtures;
import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraContractProperties;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

// Contra un Redis real: lo que se prueba es cómo el adapter usa los comandos y traduce lo que vuelve
@Testcontainers
class CatedraSyncSourceAdapterTest {

    private static final String METADATA = "catedra:sync:metadata";
    private static final String CATEGORIES = "catedra:sync:professional-categories";
    private static final String PROFESSIONALS = "catedra:sync:professionals";
    private static final String SCHEDULES = "catedra:sync:weekly-schedules";

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate template;

    private CatedraSyncSourceAdapter adapter;

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        template = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        template.delete(template.keys("*"));
        adapter = adapterOn(template);
    }

    private CatedraSyncSourceAdapter adapterOn(StringRedisTemplate redisTemplate) {
        return adapterOn(redisTemplate, CatedraContractFixtures.v1());
    }

    private CatedraSyncSourceAdapter adapterOn(StringRedisTemplate redisTemplate, CatedraContractProperties contract) {
        // Como Spring Boot: los campos que el contrato no define se ignoran
        JsonMapper jsonMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        CatedraEntityMapper entityMapper = new CatedraEntityMapper();
        return new CatedraSyncSourceAdapter(redisTemplate, jsonMapper, entityMapper,
                new RedisSyncMapper(entityMapper), contract);
    }

    private String categoryJson(long id) {
        return """
                {"id": %d, "name": "Categoria %d", "description": "Descripcion %d", "enabled": true,
                 "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}""".formatted(id, id, id);
    }

    private String professionalJson(long id, long categoryId) {
        return """
                {"id": %d, "categoryId": %d, "firstName": "Ana", "lastName": "Gomez", "enabled": false,
                 "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}""".formatted(id, categoryId);
    }

    private String scheduleJson(long id, long professionalId) {
        return """
                {"id": %d, "professionalId": %d, "dayOfWeek": "MONDAY", "startTime": "09:00:00",
                 "endTime": "12:00:00", "slotDurationMinutes": 30, "enabled": true,
                 "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}""".formatted(id, professionalId);
    }

    private void putEntity(String key, long id, String json) {
        template.opsForHash().put(key, String.valueOf(id), json);
    }

    // Las claves son configuración: si la cátedra las renombra, el adapter lee las nuevas y no las del contrato v1
    @Test
    void readsFromTheConfiguredKeysAndNotFromTheV1Names() {
        CatedraSyncSourceAdapter renamed = adapterOn(template, CatedraContractFixtures.renamed());
        template.opsForHash().putAll("nueva:meta",
                Map.of("currentVersion", "9", "oldestAvailableVersion", "8", "publishedAt", "2026-09-10T13:44:59Z"));
        template.opsForValue().set("nueva:cambios:9", """
                {"version": 9, "changes": {"professionalCategories": [], "professionals": [], "weeklySchedules": [21]}}""");
        putEntity("nueva:categorias", 1, categoryJson(1));
        putEntity("nueva:profesionales", 15, professionalJson(15, 1));
        putEntity("nueva:horarios", 21, scheduleJson(21, 15));

        assertThat(renamed.readMetadata().currentVersion()).isEqualTo(9);
        assertThat(renamed.readChanges(9)).isPresent();
        assertThat(renamed.readCategories(List.of(1L))).hasSize(1);
        assertThat(renamed.readProfessionals(List.of(15L))).hasSize(1);
        assertThat(renamed.readWeeklySchedules(List.of(21L))).hasSize(1);
        assertThatThrownBy(adapter::readMetadata).isInstanceOf(CatedraResponseException.class);
    }

    @Test
    void readsTheMetadataHash() {
        template.opsForHash().putAll(METADATA, Map.of("currentVersion", "7", "oldestAvailableVersion", "4",
                "publishedAt", "2026-09-10T13:44:59Z"));

        SyncMetadata result = adapter.readMetadata();

        assertThat(result).isEqualTo(new SyncMetadata(7, 4, Instant.parse("2026-09-10T13:44:59Z")));
    }

    @Test
    void rejectsMetadataThatTheCatedraDidNotPublish() {
        assertThatThrownBy(() -> adapter.readMetadata()).isInstanceOf(CatedraResponseException.class);
    }

    @Test
    void readsTheChangesOfAVersion() {
        template.opsForValue().set("catedra:sync:changes:7", """
                {"version": 7, "changes": {"professionalCategories": [], "professionals": [15], "weeklySchedules": [21, 22]}}""");

        assertThat(adapter.readChanges(7)).contains(new CatalogChanges(7, List.of(), List.of(15L), List.of(21L, 22L)));
    }

    @Test
    void returnsEmptyWhenTheVersionIsNoLongerRetained() {
        assertThat(adapter.readChanges(3)).isEmpty();
    }

    @Test
    void rejectsChangesThatAreNotJson() {
        template.opsForValue().set("catedra:sync:changes:7", "esto no es json");

        assertThatThrownBy(() -> adapter.readChanges(7))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("catedra:sync:changes:7");
    }

    @Test
    void rejectsChangesThatCarryAnotherVersion() {
        template.opsForValue().set("catedra:sync:changes:7", """
                {"version": 6, "changes": {"professionalCategories": [], "professionals": [], "weeklySchedules": []}}""");

        assertThatThrownBy(() -> adapter.readChanges(7))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("versión 6");
    }

    @Test
    void readsEveryFieldOfACategory() {
        putEntity(CATEGORIES, 1, categoryJson(1));

        List<ProfessionalCategory> result = adapter.readCategories(List.of(1L));

        assertThat(result).singleElement().satisfies(category -> {
            assertThat(category.getId()).isEqualTo(1L);
            assertThat(category.getName()).isEqualTo("Categoria 1");
            assertThat(category.getDescription()).isEqualTo("Descripcion 1");
            assertThat(category.isEnabled()).isTrue();
            assertThat(category.getCreatedAt()).isEqualTo(Instant.parse("2026-07-01T12:00:00Z"));
            assertThat(category.getUpdatedAt()).isEqualTo(Instant.parse("2026-09-10T13:40:00Z"));
        });
    }

    @Test
    void readsEveryFieldOfAProfessional() {
        putEntity(PROFESSIONALS, 15, professionalJson(15, 1));

        List<Professional> result = adapter.readProfessionals(List.of(15L));

        assertThat(result).singleElement().satisfies(professional -> {
            assertThat(professional.getId()).isEqualTo(15L);
            assertThat(professional.getCategoryId()).isEqualTo(1L);
            assertThat(professional.getFirstName()).isEqualTo("Ana");
            assertThat(professional.getLastName()).isEqualTo("Gomez");
            assertThat(professional.isEnabled()).isFalse();
        });
    }

    @Test
    void readsEveryFieldOfAWeeklySchedule() {
        putEntity(SCHEDULES, 21, scheduleJson(21, 15));

        List<WeeklySchedule> result = adapter.readWeeklySchedules(List.of(21L));

        assertThat(result).singleElement().satisfies(schedule -> {
            assertThat(schedule.getId()).isEqualTo(21L);
            assertThat(schedule.getProfessionalId()).isEqualTo(15L);
            assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 0));
            assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
            assertThat(schedule.isEnabled()).isTrue();
        });
    }

    @Test
    void readsOnlyTheRequestedIdsInTheRequestedOrder() {
        putEntity(CATEGORIES, 1, categoryJson(1));
        putEntity(CATEGORIES, 2, categoryJson(2));
        putEntity(CATEGORIES, 3, categoryJson(3));

        List<ProfessionalCategory> result = adapter.readCategories(List.of(3L, 1L));

        assertThat(result).extracting(ProfessionalCategory::getId).containsExactly(3L, 1L);
    }

    @Test
    void asksOnlyOnceForARepeatedId() {
        putEntity(CATEGORIES, 1, categoryJson(1));

        assertThat(adapter.readCategories(List.of(1L, 1L))).hasSize(1);
    }

    @Test
    void returnsAnEmptyListWhenNoIdsAreRequested() {
        assertThat(adapter.readCategories(List.of())).isEmpty();
        assertThat(adapter.readProfessionals(List.of())).isEmpty();
        assertThat(adapter.readWeeklySchedules(List.of())).isEmpty();
    }

    @Test
    void rejectsARequestedIdThatTheCatedraDoesNotPublish() {
        putEntity(CATEGORIES, 1, categoryJson(1));

        assertThatThrownBy(() -> adapter.readCategories(List.of(1L, 99L)))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("99");
    }

    @Test
    void rejectsAnEntityThatIsNotJson() {
        putEntity(PROFESSIONALS, 15, "{no es json");

        assertThatThrownBy(() -> adapter.readProfessionals(List.of(15L)))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining(PROFESSIONALS);
    }

    @Test
    void rejectsAnEntityWithoutAMandatoryField() {
        putEntity(CATEGORIES, 1, """
                {"id": 1, "description": "x", "enabled": true,
                 "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}""");

        assertThatThrownBy(() -> adapter.readCategories(List.of(1L)))
                .isInstanceOf(CatedraResponseException.class)
                .hasMessageContaining("name");
    }

    @Test
    void ignoresFieldsThatTheContractDoesNotDefine() {
        putEntity(CATEGORIES, 1, categoryJson(1).replace("{", "{\"campoNuevo\": \"x\", "));

        assertThat(adapter.readCategories(List.of(1L))).hasSize(1);
    }

    @Test
    void failsAsUnavailableWhenRedisDoesNotAnswer() {
        // Puerto 1: nadie escucha, la conexión se rechaza en el acto
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2)).build();
        LettuceConnectionFactory unreachable = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 1), client);
        unreachable.afterPropertiesSet();
        try {
            CatedraSyncSourceAdapter offline = adapterOn(new StringRedisTemplate(unreachable));

            assertThatThrownBy(offline::readMetadata).isInstanceOf(CatedraUnavailableException.class);
            assertThatThrownBy(() -> offline.readChanges(7)).isInstanceOf(CatedraUnavailableException.class);
            assertThatThrownBy(() -> offline.readCategories(List.of(1L)))
                    .isInstanceOf(CatedraUnavailableException.class);
        } finally {
            unreachable.destroy();
        }
    }
}
