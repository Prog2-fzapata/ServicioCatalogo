package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;
import lombok.RequiredArgsConstructor;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@Transactional
class AuthenticationIntegrationTest {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final JwtDecoder jwtDecoder;
    private final SecretKey jwtSecretKey;
    private final PasswordHasher passwordHasher;

    private void register(String login, String email) throws Exception {
        register(login, email, "clave123");
    }

    private void register(String login, String email, String password) throws Exception {
        String body = "{\"login\":\"" + login + "\",\"password\":\"" + password + "\",\"email\":\""
                + email + "\",\"firstName\":\"Maria\",\"lastName\":\"Perez\"}";
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private ResultActions authenticate(String username, String password, boolean rememberMe) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"rememberMe\":"
                + rememberMe + "}";
        return mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String login(String username) throws Exception {
        String response = authenticate(username, "clave123", false)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id_token");
    }

    private ResultActions getAccount(String token) throws Exception {
        return mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + token));
    }

    private ResultActions saveAccount(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/account").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String tokenSignedWith(SecretKey key, MacAlgorithm algorithm, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("maria.perez")
                .claim("uid", 1L)
                .claim("auth", "ROLE_USER")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims)).getTokenValue();
    }

    private String tokenSignedWith(SecretKey key, MacAlgorithm algorithm) {
        Instant now = Instant.now();
        return tokenSignedWith(key, algorithm, now, now.plus(1, ChronoUnit.HOURS));
    }

    private SecretKey randomKey() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return new SecretKeySpec(bytes, "HS512");
    }

    private String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }

    @Test
    void registerThenAuthenticateThenReadAndUpdateTheAccount() throws Exception {
        register("maria.perez", "maria@mail.com");

        String token = login("maria.perez");

        getAccount(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("maria.perez"))
                .andExpect(jsonPath("$.email").value("maria@mail.com"))
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"));

        saveAccount(token, "{\"firstName\":\"Mariana\",\"lastName\":\"Gomez\",\"email\":\"Nueva@Mail.com\","
                + "\"langKey\":\"en\"}").andExpect(status().isOk());

        getAccount(token)
                .andExpect(jsonPath("$.login").value("maria.perez"))
                .andExpect(jsonPath("$.firstName").value("Mariana"))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.email").value("nueva@mail.com"))
                .andExpect(jsonPath("$.langKey").value("en"));
        // Cambiar los datos no toca la contraseña ni los roles
        authenticate("maria.perez", "clave123", false).andExpect(status().isOk());
        authenticate("nueva@mail.com", "clave123", false).andExpect(status().isOk());
    }

    @Test
    void theIssuedTokenCarriesTheStoredUserIdAndRole() throws Exception {
        register("maria.perez", "maria@mail.com");
        Long id = jdbcTemplate.queryForObject("select id from users where login = 'maria.perez'", Long.class);

        Jwt jwt = jwtDecoder.decode(login("maria.perez"));

        assertThat(jwt.getSubject()).isEqualTo("maria.perez");
        assertThat(((Number) jwt.getClaim("uid")).longValue()).isEqualTo(id);
        assertThat(jwt.<String>getClaim("auth")).isEqualTo("ROLE_USER");
        assertThat(jwt.getClaims()).doesNotContainKeys("firstName", "lastName", "email");
    }

    @Test
    void loginIsAcceptedByEmailAndIgnoringCase() throws Exception {
        register("maria.perez", "maria@mail.com");

        authenticate("MARIA@Mail.com", "clave123", false).andExpect(status().isOk());
        authenticate("Maria.Perez", "clave123", false).andExpect(status().isOk());
    }

    // La API ya no deja crear esta colisión (ver más abajo), pero podría llegar por otra vía o por dos
    // registros simultáneos: el login igual debe resolverse entrando el dueño de la contraseña
    @Test
    void textThatIsTheLoginOfOneUserAndTheEmailOfAnotherLetsEachEnterWithTheirOwnPassword() throws Exception {
        String insert = "insert into users (login, password_hash, email, activated) values (?, ?, ?, true)";
        jdbcTemplate.update(insert, "ana@mail.com", passwordHasher.hash("clave-ana"), "ana-real@mail.com");
        jdbcTemplate.update(insert, "bruno", passwordHasher.hash("clave-bruno"), "ana@mail.com");
        Long anaId = jdbcTemplate.queryForObject("select id from users where login = 'ana@mail.com'", Long.class);
        Long brunoId = jdbcTemplate.queryForObject("select id from users where login = 'bruno'", Long.class);

        String anaResponse = authenticate("ana@mail.com", "clave-ana", false).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String brunoResponse = authenticate("ana@mail.com", "clave-bruno", false).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(((Number) jwtDecoder.decode(JsonPath.read(anaResponse, "$.id_token")).getClaim("uid")).longValue())
                .isEqualTo(anaId);
        assertThat(((Number) jwtDecoder.decode(JsonPath.read(brunoResponse, "$.id_token")).getClaim("uid")).longValue())
                .isEqualTo(brunoId);
        authenticate("ana@mail.com", "otra-clave", false).andExpect(status().isUnauthorized());
    }

    @Test
    void registeringALoginThatIsTheEmailOfAnotherUserIsRejected() throws Exception {
        register("ana", "ana@mail.com");

        String body = "{\"login\":\"Ana@Mail.com\",\"password\":\"clave123\",\"email\":\"otro@mail.com\"}";
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LOGIN_ALREADY_USED"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
    }

    @Test
    void registeringAnEmailThatIsTheLoginOfAnotherUserIsRejected() throws Exception {
        register("ana@mail.com", "ana-real@mail.com");

        String body = "{\"login\":\"bruno\",\"password\":\"clave123\",\"email\":\"ANA@mail.com\"}";
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void updatingToAnEmailThatIsTheLoginOfAnotherUserIsRejected() throws Exception {
        register("ana@mail.com", "ana-real@mail.com");
        register("bruno", "bruno@mail.com");

        saveAccount(login("bruno"), "{\"email\":\"ana@mail.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void aUserCanRegisterWithTheSameTextAsLoginAndEmail() throws Exception {
        register("ana@mail.com", "ana@mail.com");

        authenticate("ana@mail.com", "clave123", false).andExpect(status().isOk());
    }

    @Test
    void rememberMeTokenLastsLongerThanTheDefaultOne() throws Exception {
        register("maria.perez", "maria@mail.com");
        Jwt normal = jwtDecoder.decode(login("maria.perez"));
        String response = authenticate("maria.perez", "clave123", true).andReturn().getResponse().getContentAsString();
        Jwt remembered = jwtDecoder.decode(JsonPath.read(response, "$.id_token"));

        assertThat(Duration.between(normal.getIssuedAt(), normal.getExpiresAt())).hasHours(24);
        assertThat(Duration.between(remembered.getIssuedAt(), remembered.getExpiresAt())).hasDays(30);
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSame401() throws Exception {
        register("maria.perez", "maria@mail.com");

        authenticate("maria.perez", "incorrecta", false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        authenticate("nadie", "clave123", false)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void inactiveUserCannotAuthenticate() throws Exception {
        jdbcTemplate.update("insert into users (login, password_hash, email, activated) values (?, ?, ?, false)",
                "inactiva", passwordHasher.hash("clave123"), "inactiva@mail.com");

        authenticate("inactiva", "clave123", false).andExpect(status().isUnauthorized());
    }

    @Test
    void updatingToTheEmailOfAnotherUserIsRejected() throws Exception {
        register("maria.perez", "maria@mail.com");
        register("juan.gomez", "juan@mail.com");

        saveAccount(login("maria.perez"), "{\"email\":\"JUAN@mail.com\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
    }

    @Test
    void protectedEndpointsRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthAndAuthenticateStayPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        authenticate("nadie", "clave123", false).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIsRejected() throws Exception {
        getAccount("esto.no.es-un-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        getAccount(tokenSignedWith(randomKey(), MacAlgorithm.HS512)).andExpect(status().isUnauthorized());
    }

    // Aunque la firma sea correcta, el servicio solo acepta HS512
    @Test
    void tokenSignedWithTheRightKeyButAnotherAlgorithmIsRejected() throws Exception {
        getAccount(tokenSignedWith(jwtSecretKey, MacAlgorithm.HS256)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant longAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        String expired = tokenSignedWith(jwtSecretKey, MacAlgorithm.HS512, longAgo,
                longAgo.plus(1, ChronoUnit.HOURS));

        getAccount(expired).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutSignatureIsRejected() throws Exception {
        String unsigned = base64Url("{\"alg\":\"none\"}") + "."
                + base64Url("{\"sub\":\"maria.perez\",\"uid\":1,\"auth\":\"ROLE_USER\",\"exp\":9999999999}") + ".";

        getAccount(unsigned).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithAlteredPayloadIsRejected() throws Exception {
        register("maria.perez", "maria@mail.com");
        String[] parts = login("maria.perez").split("\\.");
        String forgedPayload = base64Url("{\"sub\":\"maria.perez\",\"uid\":1,\"auth\":\"ROLE_ADMIN\",\"exp\":9999999999}");

        getAccount(parts[0] + "." + forgedPayload + "." + parts[2]).andExpect(status().isUnauthorized());
    }
}
