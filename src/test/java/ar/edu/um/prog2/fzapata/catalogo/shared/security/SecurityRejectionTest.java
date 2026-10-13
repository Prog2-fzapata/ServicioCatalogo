package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.SecurityRejectionTest.SecurityProbeController;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.GlobalExceptionHandler;
import ar.edu.um.prog2.fzapata.catalogo.shared.web.ProblemDetailsFactory;
import lombok.RequiredArgsConstructor;

// Ningún endpoint real pide un rol todavía: un controller de prueba permite ejercitar el 403
@WebMvcTest(SecurityProbeController.class)
@Import({SecurityProbeController.class, SecurityConfiguration.class, JwtConfiguration.class,
        ProblemDetailsSecurityHandler.class, ProblemDetailsFactory.class, GlobalExceptionHandler.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class SecurityRejectionTest {

    @RestController
    static class SecurityProbeController {

        @GetMapping("/api/probe")
        String authenticated() {
            return "ok";
        }

        @GetMapping("/api/probe/admin")
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
        String adminOnly() {
            return "ok";
        }
    }

    private final MockMvc mockMvc;
    private final SecretKey jwtSecretKey;

    private String token(SecretKey key, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("maria").issuedAt(issuedAt).expiresAt(expiresAt)
                .claim(JwtClaims.AUTHORITIES, "ROLE_USER").build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims))
                .getTokenValue();
    }

    private ResultActions callWithBearer(String bearer) throws Exception {
        return mockMvc.perform(get("/api/probe").header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer));
    }

    // Los cuatro rechazos del filtro deben verse igual: no se revela por qué falló el token
    private void expectUnauthorizedProblem(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.detail").value("Falta el token de autenticación o no es válido"))
                .andExpect(jsonPath("$.path").value("/api/probe"));
    }

    @Test
    void requestWithoutTokenIsRejectedWithProblemJson() throws Exception {
        expectUnauthorizedProblem(mockMvc.perform(get("/api/probe")));
    }

    @Test
    void malformedTokenIsRejectedWithProblemJson() throws Exception {
        expectUnauthorizedProblem(callWithBearer("esto-no-es-un-jwt"));
    }

    @Test
    void expiredTokenIsRejectedWithProblemJson() throws Exception {
        Instant longAgo = Instant.now().minus(2, ChronoUnit.DAYS);
        expectUnauthorizedProblem(callWithBearer(token(jwtSecretKey, longAgo, longAgo.plus(1, ChronoUnit.HOURS))));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejectedWithProblemJson() throws Exception {
        SecretKey otherKey = new SecretKeySpec(new byte[64], "HmacSHA512");
        Instant now = Instant.now();

        expectUnauthorizedProblem(callWithBearer(token(otherKey, now, now.plus(1, ChronoUnit.HOURS))));
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        Instant now = Instant.now();

        callWithBearer(token(jwtSecretKey, now, now.plus(1, ChronoUnit.HOURS))).andExpect(status().isOk());
    }

    // Todo lo que no es expresamente público exige token, también las rutas que no existen
    @Test
    void unknownPathWithoutTokenIsRejectedBeforeAnyRouting() throws Exception {
        mockMvc.perform(get("/api/no-existe")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserWithoutTheRequiredRoleGetsForbiddenProblemJson() throws Exception {
        mockMvc.perform(get("/api/probe/admin").with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("No tiene permiso para realizar esta operación"))
                .andExpect(jsonPath("$.path").value("/api/probe/admin"));
    }

    @Test
    void userWithTheRequiredRoleIsAccepted() throws Exception {
        mockMvc.perform(get("/api/probe/admin").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }
}
