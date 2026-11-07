package ar.edu.um.prog2.fzapata.catalogo;

import static ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.FakeHttpServer.cutConnection;
import static ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.FakeHttpServer.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.client.FakeHttpServer;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// Criterio del issue #17: ni las contraseñas ni los JWT ni las claves llegan a un log. Cada test corre un flujo real
// (PostgreSQL y Redis reales, la cátedra como servidor HTTP local), captura TODO lo que se escribe (mensaje, campos,
// contexto y traza completa de cada excepción, con sus causas) y comprueba que ningún valor sentinela aparece. Antes
// comprueba que el valor sí viajó por el flujo: sin eso el test pasaría igual si el flujo no lo hubiera usado.
// La verificación de versión corre sola cada 200 ms contra Redis (que rechaza la clave) y la cátedra simulada
@SpringBootTest(properties = {
        "catedra.sync.check-enabled=true",
        "catedra.sync.check-interval=200ms",
        "catedra.sync.initial-delay=0s"})
@AutoConfigureMockMvc
@Testcontainers
@Import(SecretsNotInLogsIntegrationTest.DatabaseWithPassword.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class SecretsNotInLogsIntegrationTest {

    // El propio contenedor, para que la contraseña de la base sea una de las que se buscan en el log
    @TestConfiguration(proxyBeanMethods = false)
    static class DatabaseWithPassword {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer(DockerImageName.parse("postgres:18")).withUsername("catalogo")
                    .withPassword(DATABASE_PASSWORD);
        }
    }

    private static final String DATABASE_PASSWORD = "DB-PASSWORD-SENTINEL-5e2d";
    private static final String LOGIN = "login-sentinel";
    private static final String EMAIL = "sentinel@mail.com";
    private static final String PASSWORD = "PASSWORD-SENTINEL-8b1f";
    private static final String WRONG_PASSWORD = "WRONG-PASSWORD-SENTINEL-2c9a";
    // Un JWT de verdad tiene tres tramos: el primero es alfanumérico y es lo que un mensaje de error de JSON repite
    private static final String CATEDRA_JWT = "eyJSENTINELCATEDRA77c1e0.cGF5bG9hZA.c2lnbmF0dXJl";
    private static final String REDIS_PASSWORD = "REDIS-PASSWORD-SENTINEL-6d3e";
    private static final String OTHER_REDIS_PASSWORD = "la-clave-que-si-pide-el-redis";
    private static final String MARKER = "RESPUESTA-SIMULADA-ENTREGADA";
    private static final Duration WAIT = Duration.ofSeconds(30);

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379).withCommand("redis-server", "--requirepass", OTHER_REDIS_PASSWORD);

    private static final FakeHttpServer CATEDRA = newFakeServer();

    private static final String SNAPSHOT = """
            {"snapshotVersion": 7, "generatedAt": "2026-09-10T13:44:59Z",
             "professionalCategories": [
               {"id": 1, "name": "Clinica medica", "description": "Atencion clinica general", "enabled": true,
                "createdAt": "2026-07-01T12:00:00Z", "updatedAt": "2026-09-10T13:40:00Z"}],
             "professionals": [], "weeklySchedules": []}""";

    private final MockMvc mockMvc;
    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockitoSpyBean
    private UserRepository userRepository;

    private LogCapture capture;

    private static FakeHttpServer newFakeServer() {
        try {
            return new FakeHttpServer();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void pointTheClientsToTheSimulatedServices(DynamicPropertyRegistry registry) {
        registry.add("catedra.base-url", CATEDRA::baseUrl);
        registry.add("catedra.jwt", () -> CATEDRA_JWT);
        registry.add("CATEDRA_REDIS_HOST", REDIS::getHost);
        registry.add("CATEDRA_REDIS_PORT", () -> REDIS.getMappedPort(6379));
        registry.add("CATEDRA_REDIS_USERNAME", () -> "grp_sentinel");
        registry.add("CATEDRA_REDIS_PASSWORD", () -> REDIS_PASSWORD);
    }

    @AfterAll
    static void stopTheSimulatedCatedra() {
        CATEDRA.close();
    }

    @BeforeEach
    void captureTheLog() {
        CATEDRA.reset();
        jdbc.sql("DELETE FROM users").update();
        jdbc.sql("DELETE FROM weekly_schedules").update();
        jdbc.sql("DELETE FROM professionals").update();
        jdbc.sql("DELETE FROM professional_categories").update();
        jdbc.sql("""
                UPDATE sync_state SET local_version = 0, last_sync_at = NULL, last_sync_type = NULL,
                                      last_status = NULL, last_error = NULL""").update();
        capture = LogCapture.ofEverything();
    }

    @AfterEach
    void releaseTheLog() {
        capture.close();
    }

    private List<String> logged() {
        return capture.events().stream().map(LogCapture::everythingIn).toList();
    }

    // Los valores que nunca deben aparecer, con las partes por las que un mensaje de error podría repetirlos
    private List<String> secrets(String... extra) {
        List<String> secrets = new ArrayList<>(List.of(DATABASE_PASSWORD, LOGIN, EMAIL, PASSWORD,
                WRONG_PASSWORD, CATEDRA_JWT, "eyJSENTINELCATEDRA77c1e0", "cGF5bG9hZA", REDIS_PASSWORD,
                OTHER_REDIS_PASSWORD, jwtSecret));
        secrets.addAll(List.of(extra));
        return secrets;
    }

    private void assertNoSecretIsLogged(String... extra) {
        String everything = String.join("\n", logged());
        for (String secret : secrets(extra)) {
            assertThat(everything).as("el log no debe contener %s", secret).doesNotContain(secret);
        }
    }

    private void assertSomethingWasLogged(String fragment) {
        assertThat(logged()).as("el flujo debía dejar un log con '%s'", fragment)
                .anyMatch(line -> line.contains(fragment));
    }

    // Una respuesta del guion de la cátedra que deja una marca en el log, para saber que ya se entregó y esperar la
    // línea que la ronda escribe después. Las rondas siguen corriendo solas: sin esto no se sabría cuál se registró
    private FakeHttpServer.Behavior marked(FakeHttpServer.Behavior behavior) {
        return exchange -> {
            LoggerFactory.getLogger("catedra-simulada").info(MARKER);
            behavior.handle(exchange);
        };
    }

    private void waitForTheLogAfterTheMarker(String fragment) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            List<ILoggingEvent> events = capture.events();
            int marker = indexOfMarker(events);
            if (marker >= 0 && events.stream().skip(marker + 1)
                    .anyMatch(event -> LogCapture.everythingIn(event).contains(fragment))) {
                return;
            }
            Thread.sleep(100);
        }
        assertSomethingWasLogged(fragment);
    }

    private static int indexOfMarker(List<ILoggingEvent> events) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getFormattedMessage().contains(MARKER)) {
                return i;
            }
        }
        return -1;
    }

    // La cátedra que repite en su error lo que recibió, incluido el JWT técnico con el que se la llamó
    private static String echoing(String code) {
        return "{\"code\":\"" + code + "\",\"detail\":\"Bearer " + CATEDRA_JWT + " no es válido\"}";
    }

    private ResultActions register(String login, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + login + "\",\"email\":\"" + email + "\",\"password\":\"" + password
                        + "\"}"));
    }

    private ResultActions authenticate(String login, String password) throws Exception {
        return mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + login + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions asUser(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private String tokenOf(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id_token").asString();
    }

    private String lastError() {
        return jdbc.sql("SELECT last_error FROM sync_state WHERE id = 1").query(String.class).single();
    }

    @Test
    void registeringAuthenticatingAndUsingTheTokenLogsNeitherThePasswordNorTheTokenNorTheData() throws Exception {
        register(LOGIN, EMAIL, PASSWORD).andExpect(status().isCreated());
        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE login = ?").param(LOGIN).query(Long.class).single())
                .isEqualTo(1);

        ResultActions authenticated = authenticate(LOGIN, PASSWORD).andExpect(status().isOk());
        String token = tokenOf(authenticated);
        assertThat(token).isNotBlank();
        asUser(get("/api/account"), token).andExpect(status().isOk()).andExpect(jsonPath("$.login").value(LOGIN));

        assertNoSecretIsLogged(token);
    }

    @Test
    void aRejectedLoginLeavesAWarningWithoutTheLoginNorAnyPassword() throws Exception {
        register(LOGIN, EMAIL, PASSWORD).andExpect(status().isCreated());

        authenticate(LOGIN, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        authenticate("otro-login-sentinel", WRONG_PASSWORD).andExpect(status().isUnauthorized());

        assertSomethingWasLogged("Credenciales rechazadas");
        assertNoSecretIsLogged("otro-login-sentinel");
    }

    // PostgreSQL repite en su mensaje la fila que chocó (login, email y hash): el log no debe copiarla
    @Test
    void anUnforeseenIntegrityViolationLogsNeitherTheLoginNorTheEmail() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate key: Key (email)=(" + EMAIL + ") Failing row contains ("
                + LOGIN + ", " + PASSWORD + ")")).when(userRepository).create(any());

        register(LOGIN, EMAIL, PASSWORD).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        assertSomethingWasLogged("Violación de integridad no prevista");
        assertNoSecretIsLogged();
    }

    @Test
    void aCatedraThatRepeatsTheTechnicalJwtInItsServerErrorDoesNotLeakItInTheLog() throws Exception {
        CATEDRA.thenRespond(marked(status(500, echoing("INTERNAL_ERROR"))));

        waitForTheLogAfterTheMarker("La verificación de versión falló");

        assertNoSecretIsLogged();
        assertThat(lastError()).isNotNull().doesNotContain(secrets().toArray(String[]::new));
    }

    @Test
    void aCatedraThatRejectsTheTechnicalJwtDoesNotLeakItInTheLog() throws Exception {
        CATEDRA.thenRespond(marked(status(403, echoing("FORBIDDEN"))));

        waitForTheLogAfterTheMarker("La verificación de versión falló");

        assertNoSecretIsLogged();
        assertThat(lastError()).isNotNull().doesNotContain(secrets().toArray(String[]::new));
    }

    // Un 200 cuyo cuerpo no es JSON y empieza con el JWT: el mensaje de error del parser repite el primer tramo
    @Test
    void aBodyThatIsNotJsonAndStartsWithTheJwtDoesNotLeakItInTheLog() throws Exception {
        CATEDRA.thenRespond(marked(status(200, CATEDRA_JWT + " no es json")));

        waitForTheLogAfterTheMarker("La verificación de versión falló");

        assertNoSecretIsLogged();
        assertThat(lastError()).isNotNull().doesNotContain(secrets().toArray(String[]::new));
    }

    @Test
    void aCatedraThatCutsTheConnectionDoesNotLeakTheJwtInTheLog() throws Exception {
        CATEDRA.thenRespond(marked(cutConnection()));

        waitForTheLogAfterTheMarker("La verificación de versión falló");

        assertNoSecretIsLogged();
    }

    // Con Redis rechazando la clave la ronda cae al snapshot, lo aplica y lo dice (la traza de sincronización)
    @Test
    void aSnapshotAppliedBecauseRedisRefusedTheKeyIsLoggedWithoutAnyKey() throws Exception {
        CATEDRA.thenRespond(marked(status(200, SNAPSHOT)));

        waitForTheLogAfterTheMarker("Verificación de versión: aplicado");

        assertSomethingWasLogged("no se pudo leer Redis");
        assertNoSecretIsLogged();
    }
}
