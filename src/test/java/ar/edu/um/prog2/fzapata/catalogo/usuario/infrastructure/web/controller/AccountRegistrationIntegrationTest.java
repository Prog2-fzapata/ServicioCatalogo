package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.handler.UserExceptionHandler;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@Transactional
class AccountRegistrationIntegrationTest {

    private static final String INSERT_USER =
            "insert into users (login, password_hash, email, activated) values (?, 'x', ?, true)";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final UserExceptionHandler userExceptionHandler;

    private ResultActions register(String login, String email) throws Exception {
        String body = "{\"login\":\"" + login + "\",\"password\":\"clave123\",\"email\":\"" + email + "\"}";
        return mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int countUsers() {
        return jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
    }

    private ResponseEntity<ProblemDetail> translate(Throwable thrown) {
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
        return userExceptionHandler.handleDataIntegrityViolation(
                (DataIntegrityViolationException) thrown, new MockHttpServletRequest("POST", "/api/register"));
    }

    @Test
    void registerStoresAnActiveUserWithTheUserRoleAndABcryptHash() throws Exception {
        register("Maria.Perez", "Maria@Mail.com").andExpect(status().isCreated());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select password_hash, activated, email, lang_key from users where login = 'maria.perez'");
        assertThat((String) row.get("password_hash")).startsWith("$2a$10$").doesNotContain("clave123");
        assertThat(row).containsEntry("activated", true)
                .containsEntry("email", "maria@mail.com")
                .containsEntry("lang_key", "es");

        // Dentro de la transacción del test, Hibernate escribe los roles recién al hacer flush
        entityManager.flush();
        List<String> roles = jdbcTemplate.queryForList(
                "select authority_name from user_authorities ua join users u on u.id = ua.user_id "
                        + "where u.login = 'maria.perez'", String.class);
        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    void registerRejectsAnExistingLoginIgnoringCase() throws Exception {
        register("maria.perez", "maria@mail.com").andExpect(status().isCreated());

        register("MARIA.PEREZ", "otra@mail.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_ALREADY_USED"));

        assertThat(countUsers()).isEqualTo(1);
    }

    @Test
    void registerRejectsAnExistingEmailIgnoringCase() throws Exception {
        register("maria.perez", "maria@mail.com").andExpect(status().isCreated());

        register("otra.persona", "MARIA@mail.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));

        assertThat(countUsers()).isEqualTo(1);
    }

    // Si dos registros simultáneos pasan el chequeo previo, esta es la excepción real que lanza la base
    @Test
    void realLoginUniqueViolationFromTheDatabaseIsTranslated() {
        jdbcTemplate.update(INSERT_USER, "ana", "ana@mail.com");

        Throwable thrown = catchThrowable(() -> jdbcTemplate.update(INSERT_USER, "ana", "otro@mail.com"));

        ResponseEntity<ProblemDetail> response = translate(thrown);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("code", "LOGIN_ALREADY_USED");
    }

    @Test
    void realEmailUniqueViolationFromTheDatabaseIsTranslated() {
        jdbcTemplate.update(INSERT_USER, "ana", "ana@mail.com");

        Throwable thrown = catchThrowable(() -> jdbcTemplate.update(INSERT_USER, "otra", "ana@mail.com"));

        ResponseEntity<ProblemDetail> response = translate(thrown);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties()).containsEntry("code", "EMAIL_ALREADY_USED");
    }
}
