package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import ar.edu.um.prog2.fzapata.catalogo.shared.security.JwtProperties;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;

class JwtTokenIssuerTest {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final Duration REMEMBER_ME_VALIDITY = Duration.ofDays(30);

    private Instant now;
    private JwtDecoder decoder;
    private JwtTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        SecretKey key = new SecretKeySpec(bytes, MacAlgorithm.HS512.getName());
        // El decoder real valida vigencia contra la hora actual, así que el reloj fijo parte de ahora
        now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
        issuer = new JwtTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                new JwtProperties("no-se-usa", TOKEN_VALIDITY, REMEMBER_ME_VALIDITY),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private User user() {
        return User.builder()
                .id(42L)
                .login("maria.perez")
                .firstName("Maria")
                .lastName("Perez")
                .email("maria@mail.com")
                .passwordHash("hash")
                .authorities(Set.of("ROLE_USER", "ROLE_ADMIN"))
                .build();
    }

    @Test
    void tokenCarriesSubjectUidAuthoritiesAndDates() {
        Jwt jwt = decoder.decode(issuer.issueToken(user(), false));

        assertThat(jwt.getSubject()).isEqualTo("maria.perez");
        assertThat(((Number) jwt.getClaim("uid")).longValue()).isEqualTo(42L);
        assertThat(jwt.<String>getClaim("auth")).isEqualTo("ROLE_ADMIN ROLE_USER");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(TOKEN_VALIDITY));
    }

    @Test
    void rememberMeUsesTheLongerValidity() {
        Jwt jwt = decoder.decode(issuer.issueToken(user(), true));

        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(REMEMBER_ME_VALIDITY));
    }

    @Test
    void tokenIsSignedWithHs512AndHasNoPersonalDataBeyondTheLogin() {
        Jwt jwt = decoder.decode(issuer.issueToken(user(), false));

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS512");
        assertThat(jwt.getClaims().keySet()).containsExactlyInAnyOrder("sub", "uid", "auth", "iat", "exp");
    }
}
