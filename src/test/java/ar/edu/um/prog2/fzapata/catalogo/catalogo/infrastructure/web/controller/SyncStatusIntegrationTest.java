package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import lombok.RequiredArgsConstructor;

// Todo real: registro y login de un usuario, el filtro JWT y PostgreSQL. Cada test se deshace al terminar
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@Transactional
class SyncStatusIntegrationTest {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final RecordSyncFailureUseCase recordSyncFailureUseCase;

    private String token;

    @BeforeEach
    void registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"login":"maria.estado","password":"clave123","email":"maria.estado@mail.com",
                 "firstName":"Maria","lastName":"Perez"}""")).andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"maria.estado\",\"password\":\"clave123\",\"rememberMe\":false}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = JsonPath.read(response, "$.id_token");
    }

    private void givenSyncStateNeverUsed() {
        jdbcTemplate.update("""
                UPDATE sync_state SET local_version = 0, last_sync_at = NULL, last_sync_type = NULL,
                                      last_status = NULL, last_error = NULL""");
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void anAuthenticatedUserSeesVersionZeroWhenNothingWasSynchronizedYet() throws Exception {
        givenSyncStateNeverUsed();

        mockMvc.perform(get("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localVersion").value(0))
                .andExpect(jsonPath("$.lastStatus").value(nullValue()))
                .andExpect(jsonPath("$.lastError").value(nullValue()));
    }

    @Test
    void showsTheLastSuccessfulSynchronization() throws Exception {
        givenSyncStateNeverUsed();
        jdbcTemplate.update("""
                UPDATE sync_state SET local_version = 7, last_sync_at = TIMESTAMPTZ '2026-10-27 19:49:12+00',
                                      last_sync_type = 'INCREMENTAL', last_status = 'OK'""");

        mockMvc.perform(get("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localVersion").value(7))
                .andExpect(jsonPath("$.lastSyncAt").value("2026-10-27T19:49:12Z"))
                .andExpect(jsonPath("$.lastSyncType").value("INCREMENTAL"))
                .andExpect(jsonPath("$.lastStatus").value("OK"))
                .andExpect(jsonPath("$.lastError").value(nullValue()));
    }

    @Test
    void showsTheMessageOfAnErrorOfTheCatedra() throws Exception {
        givenSyncStateNeverUsed();
        recordSyncFailureUseCase.recordFailure(SyncType.FULL, new CatedraUnavailableException("sin conexión", null));

        mockMvc.perform(get("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastStatus").value("ERROR"))
                .andExpect(jsonPath("$.lastSyncType").value("FULL"))
                .andExpect(jsonPath("$.lastError").value("CatedraUnavailableException: sin conexión"));
    }

    // El criterio de CAT-15 de punta a punta: un error de la base trae la sentencia SQL en su mensaje, y eso no
    // debe llegar ni a sync_state ni a la respuesta
    @Test
    void neverExposesTheSqlOrTheValuesOfADatabaseError() throws Exception {
        givenSyncStateNeverUsed();
        recordSyncFailureUseCase.recordFailure(SyncType.FULL, new DataIntegrityViolationException(
                "could not execute statement [ERROR: insert or update on table \"professionals\" violates foreign "
                        + "key constraint \"fk_professionals_category\"] [insert into professionals (category_id, "
                        + "first_name, last_name) values (?, ?, ?)]"));

        mockMvc.perform(get("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastStatus").value("ERROR"))
                .andExpect(jsonPath("$.lastError").value("DataIntegrityViolationException"))
                .andExpect(content().string(not(containsString("insert into"))))
                .andExpect(content().string(not(containsString("professionals"))))
                .andExpect(content().string(not(containsString("fk_professionals_category"))));
    }

    @Test
    void doesNotSupportWriting() throws Exception {
        mockMvc.perform(post("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }
}
