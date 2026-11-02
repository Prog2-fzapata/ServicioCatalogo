package ar.edu.um.prog2.fzapata.catalogo.shared.security;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

    // HS512 es el algoritmo de JHipster; el decoder solo acepta este, así que rechaza tokens firmados con otro
    public static final MacAlgorithm ALGORITHM = MacAlgorithm.HS512;
    // HS512 exige una clave de al menos 512 bits
    static final int MIN_SECRET_BYTES = 64;

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        return secretKey(properties.secret());
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(ALGORITHM).build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    // Falla al arrancar, con un mensaje claro, en vez de emitir tokens con una clave débil
    static SecretKey secretKey(String base64Secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET no es Base64 válido; generarlo con: openssl rand -base64 64", e);
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES * 8
                    + " bits (" + MIN_SECRET_BYTES + " bytes); generarlo con: openssl rand -base64 64");
        }
        return new SecretKeySpec(bytes, ALGORITHM.getName());
    }
}
